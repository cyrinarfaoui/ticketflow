import { afterNextRender, Component, computed, DestroyRef, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap, tap, TimeoutError } from 'rxjs';
import { EventResponse } from '../../../core/services/event.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { LiveNotificationStore } from '../../../core/stores/live-notification.store';
import {
  declineLabel, digitsOnly, formatCardNumber, formatPrice, isValidCardNumber, isValidExpiry, TEST_CARDS, tokenizeTestCard,
} from '../../../core/models/payment';
import { ErrorMessages, firstError } from '../../../shared/forms/first-error';

// Les étapes du paiement, comme sur un vrai tunnel de paiement
type Step = 'form' | 'processing' | 'succeeded' | 'failed' | 'pending';

const cardNumberValidator = (control: AbstractControl): ValidationErrors | null =>
  !control.value || isValidCardNumber(control.value) ? null : { cardNumber: true };

const expiryValidator = (control: AbstractControl): ValidationErrors | null =>
  !control.value || isValidExpiry(control.value) ? null : { expiry: true };

/**
 * Fenêtre de paiement (façon Stripe Checkout).
 * Le numéro de carte reste dans le navigateur : il est transformé en jeton pm_… et SEUL le jeton part vers l'API.
 */
@Component({
  selector: 'app-payment-dialog',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './payment-dialog.html',
  styleUrl: './payment-dialog.scss',
})
export class PaymentDialog {
  private readonly reservationService = inject(ReservationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly notifications = inject(LiveNotificationStore);

  readonly event = input.required<EventResponse>();
  readonly seats = input.required<number>();
  // true si une réservation a été créée (le catalogue et le badge doivent être rafraîchis)
  readonly closed = output<boolean>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private reservationCreated = false;

  protected readonly testCards = TEST_CARDS;
  protected readonly formatCardNumber = formatCardNumber;
  protected readonly step = signal<Step>('form');
  protected readonly resultMessage = signal('');

  protected readonly unitPrice = computed(() => formatPrice(this.event().priceCents, this.event().currency));
  protected readonly total = computed(() => formatPrice(this.event().priceCents * this.seats(), this.event().currency));

  readonly form = inject(FormBuilder).nonNullable.group({
    cardNumber: ['', [Validators.required, cardNumberValidator]],
    expiry: ['', [Validators.required, expiryValidator]],
    cvc: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
  });

  private readonly errorMessages: Record<'cardNumber' | 'expiry' | 'cvc', ErrorMessages> = {
    cardNumber: {
      required: 'Le numéro de carte est obligatoire.',
      cardNumber: 'Numéro de carte invalide.',
      testMode: 'Mode test : utilisez une des cartes de test ci-dessous.',
    },
    expiry: {
      required: "La date d'expiration est obligatoire.",
      expiry: 'Date invalide ou dépassée (format MM/AA).',
    },
    cvc: {
      required: 'Le code de sécurité est obligatoire.',
      pattern: 'Le code de sécurité contient 3 chiffres.',
    },
  };

  constructor() {
    // showModal() : fenêtre modale native (le focus reste dedans, Échap la ferme, le reste de la page est inerte)
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }

  protected error(field: 'cardNumber' | 'expiry' | 'cvc'): string | null {
    return firstError(this.form.controls[field], this.errorMessages[field]);
  }

  // "4242424242424242" s'affiche "4242 4242 4242 4242" pendant la frappe
  protected onCardNumberInput(): void {
    const control = this.form.controls.cardNumber;
    control.setValue(formatCardNumber(control.value));
  }

  // "1228" s'affiche "12/28" pendant la frappe
  protected onExpiryInput(): void {
    const control = this.form.controls.expiry;
    const digits = digitsOnly(control.value).slice(0, 4);
    control.setValue(digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits);
  }

  pay(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // "Stripe.js" : numéro de carte → jeton. Le numéro ne quitte jamais le navigateur.
    const token = tokenizeTestCard(this.form.controls.cardNumber.value);
    if (!token) {
      this.form.controls.cardNumber.setErrors({ testMode: true });
      this.form.controls.cardNumber.markAsTouched();   // sinon le message ne s'affiche pas (voir firstError)
      return;
    }

    this.step.set('processing');
    this.reservationService.createReservation(this.event().id, this.seats(), token).pipe(
      tap(() => (this.reservationCreated = true)),
      // La saga Kafka décide (payment-service) : on attend son résultat
      switchMap((reservation) => this.reservationService.waitForPaymentResult(reservation.id)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (reservation) => {
        this.notifications.notifyReservation(reservation);   // la cloche du client : confirmée ou refusée
        if (reservation.status === 'CONFIRMED') {
          this.step.set('succeeded');
        } else {
          this.resultMessage.set(reservation.paymentFailureCode ? declineLabel(reservation.paymentFailureCode) : 'Paiement refusé.');
          this.step.set('failed');
        }
      },
      error: (err: unknown) => {
        if (err instanceof TimeoutError) {
          this.step.set('pending');   // la banque n'a pas encore répondu : le résultat viendra plus tard
          return;
        }
        // Erreur métier de l'API (ex. "Pas assez de places") ; les erreurs techniques sont aussi dans la bannière globale
        this.resultMessage.set((err as HttpErrorResponse).error?.error ?? "Le paiement n'a pas pu être effectué.");
        this.step.set('failed');
      },
    });
  }

  retry(): void {
    this.form.controls.cardNumber.reset();
    this.step.set('form');
  }

  close(): void {
    this.dialog().nativeElement.close();
  }

  // Fermer PUIS naviguer : avec un routerLink + (click)="close()", la fermeture détruisait
  // la fenêtre (et son lien) avant que la navigation ne parte
  goToMyReservations(): void {
    this.close();
    void this.router.navigateByUrl('/mes-reservations');
  }

  // Échap pendant la vérification : on ne ferme pas (le paiement est en cours)
  protected onCancel(event: Event): void {
    if (this.step() === 'processing') {
      event.preventDefault();
    }
  }

  protected onClose(): void {
    this.closed.emit(this.reservationCreated);
  }
}

import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EventService } from '../../../core/services/event.service';
import { EVENT_CATEGORIES, EventCategory } from '../../../core/models/event-category';
import { futureDate } from '../../../shared/validators/future-date.validator';
import { ErrorMessages, firstError } from '../../../shared/forms/first-error';
import { HttpErrorResponse } from '@angular/common/http';

// Les noms des champs du formulaire : 'title' | 'description' | 'location' | ...
type FieldName = keyof EventForm['form']['controls'];

@Component({
  selector: 'app-event-form',
  imports: [ReactiveFormsModule],
  templateUrl: './event-form.html',
  styleUrl: './event-form.scss'
})
export class EventForm {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private router = inject(Router);

  protected readonly categories = EVENT_CATEGORIES;
    // Vrai pendant l'envoi : bloque le double clic
  protected readonly submitting = signal(false);
  // Message d'erreur renvoyé par le serveur (règle métier refusée, par exemple)
  protected readonly serverError = signal<string | null>(null);

  form = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    location: ['', Validators.required],
    category: ['' as EventCategory | '', Validators.required],
    date: ['', [Validators.required, futureDate()]],
    totalSeats: [1, [Validators.required, Validators.min(1), Validators.max(100_000)]]
  });

    // Tous les messages d'erreur du formulaire, au même endroit. L'ordre = la priorité.
  private readonly errorMessages: Partial<Record<FieldName, ErrorMessages>> = {
    title: {
      required: 'Le titre est obligatoire.',
      minlength: 'Le titre doit contenir au moins 3 caractères.',
    },
    location: { required: 'Le lieu est obligatoire.' },
    category: { required: 'La catégorie est obligatoire.' },
    date: {
      required: 'La date est obligatoire.',
      pastDate: 'La date doit être dans le futur.',
    },
    totalSeats: {
      required: 'Il faut au moins 1 place.',
      min: 'Il faut au moins 1 place.',
      max: '100 000 places maximum.',
    },
  };

  // Le message à afficher pour un champ, ou null
  protected error(field: FieldName): string | null {
    const messages = this.errorMessages[field];
    return messages ? firstError(this.form.controls[field], messages) : null;
  }

   onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.submitting()) {
      return; // une requête est déjà en cours : on ignore le deuxième clic
    }
    this.submitting.set(true);
    this.serverError.set(null);

    const raw = this.form.getRawValue();
    this.eventService.createEvent({
      title: raw.title!,
      description: raw.description ?? '',
      location: raw.location!,
      category: raw.category as EventCategory,
      date: new Date(raw.date!).toISOString(),
      totalSeats: raw.totalSeats!
    }).subscribe({
      next: () => this.router.navigate(['/evenements']),
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false); // l'utilisateur peut corriger et réessayer
        this.serverError.set(err.error?.error ?? 'La création a échoué. Vérifiez les informations et réessayez.');
      },
    });
  }
}

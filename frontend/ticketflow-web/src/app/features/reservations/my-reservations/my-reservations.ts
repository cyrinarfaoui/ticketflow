import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ReservationStore } from '../../../core/stores/reservation.store';
import { MyReservation, ReservationStatus } from '../../../core/models/reservation';

// Chaque statut a un libellé ET une icône : l'information ne repose jamais sur la couleur seule (RGAA)
const STATUS_DISPLAY: Record<ReservationStatus, { label: string; icon: string }> = {
  PENDING: { label: 'En attente de paiement', icon: '⏳' },
  CONFIRMED: { label: 'Confirmée', icon: '✓' },
  CANCELLED: { label: 'Annulée', icon: '✕' },
  EXPIRED: { label: 'Expirée', icon: '⌛' },
};

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-my-reservations',
  styleUrl: './my-reservations.scss',
  templateUrl: './my-reservations.html',
})
export class MyReservations implements OnInit {

  private readonly authService = inject(AuthService);

  // Lu dans le JWT, côté navigateur : UNIQUEMENT pour l'affichage.
  // Le jeton peut être lu (et même modifié) par l'utilisateur : le backend ne fait confiance qu'à sa signature.
  protected readonly username = this.authService.username;

  // La même copie des réservations que le header et le reste de l'application
  protected readonly store = inject(ReservationStore);

  // Message d'erreur métier (409…) ; les erreurs techniques (401, 403, 5xx) sont dans la bannière globale
  readonly errorMessage = signal<string | null>(null);
  // Confirmation annoncée par les lecteurs d'écran (role="status")
  readonly successMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.store.load(); // données fraîches à l'ouverture de la page
  }

  statusDisplay(status: ReservationStatus) {
    return STATUS_DISPLAY[status];
  }

  canCancel(reservation: MyReservation): boolean {
    return reservation.status === 'PENDING' || reservation.status === 'CONFIRMED';
  }

  cancel(reservation: MyReservation): void {
    if (!confirm(`Annuler votre réservation de ${reservation.seats} place(s) pour « ${reservation.eventTitle} » ?`)) {
      return;
    }
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.store.cancel(reservation.id).subscribe({
      // Plus de reload : le store a déjà mis l'écran à jour (optimiste)
      next: () => this.successMessage.set(`Votre réservation pour « ${reservation.eventTitle} » a été annulée.`),
      // Le store a déjà fait le rollback ; on explique pourquoi
      error: (err: HttpErrorResponse) => this.errorMessage.set(err.error?.error ?? null),
    });
  }
}

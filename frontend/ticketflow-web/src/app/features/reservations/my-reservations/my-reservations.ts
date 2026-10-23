import { Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ReservationStore } from '../../../core/stores/reservation.store';
import { ReservationStatus } from '../../../core/models/reservation';
import { declineLabel } from '../../../core/models/payment';

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

  ngOnInit(): void {
    this.store.load(); // données fraîches à l'ouverture de la page
  }

  readonly declineLabel = declineLabel;

  statusDisplay(status: ReservationStatus) {
    return STATUS_DISPLAY[status];
  }
}

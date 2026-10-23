import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';
import { MyReservation } from '../models/reservation';
import { LiveNotificationStore } from './live-notification.store';

export type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error';

/**
 * L'UNIQUE copie des réservations de l'utilisateur connecté, partagée par toute l'application.
 * Les composants LISENT les signals ; seules les méthodes du store les MODIFIENT.
 */
@Injectable({ providedIn: 'root' })
export class ReservationStore {

  private readonly http = inject(HttpClient);
  private readonly notifications = inject(LiveNotificationStore);

  // État privé et modifiable…
  private readonly state = signal<MyReservation[]>([]);
  private readonly loadStatus = signal<LoadStatus>('idle');

  // … exposé en lecture seule : un composant ne peut pas faire store.reservations.set(...)
  readonly reservations = this.state.asReadonly();
  readonly status = this.loadStatus.asReadonly();

  // État dérivé : recalculé automatiquement dès que la liste change
  readonly activeCount = computed(() =>
    this.state().filter((r) => r.status === 'PENDING' || r.status === 'CONFIRMED').length,
  );

  load(): void {
    // La photo d'avant : une réservation qui a changé d'état depuis (expirée, annulée par l'organisateur…) est annoncée
    const previous = new Map(this.state().map((r) => [r.id, r.status]));
    this.loadStatus.set('loading');
    this.http.get<MyReservation[]>(`${API_BASE_URL}/reservations/me`).subscribe({
      next: (reservations) => {
        for (const reservation of reservations) {
          const before = previous.get(reservation.id);
          if (before && before !== reservation.status) {
            this.notifications.notifyReservation(reservation);
          }
        }
        this.state.set(reservations);
        this.loadStatus.set('loaded');
      },
      error: () => this.loadStatus.set('error'),
    });
  }

  clear(): void {
    this.state.set([]);
    this.loadStatus.set('idle');
  }
}
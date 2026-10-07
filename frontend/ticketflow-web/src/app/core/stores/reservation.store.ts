import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { MyReservation } from '../models/reservation';
import { ReservationResponse, ReservationService } from '../services/reservation.service';

export type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error';

/**
 * L'UNIQUE copie des réservations de l'utilisateur connecté, partagée par toute l'application.
 * Les composants LISENT les signals ; seules les méthodes du store les MODIFIENT.
 */
@Injectable({ providedIn: 'root' })
export class ReservationStore {

  private readonly http = inject(HttpClient);
  private readonly reservationService = inject(ReservationService);

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
    this.loadStatus.set('loading');
    this.http.get<MyReservation[]>(`${API_BASE_URL}/reservations/me`).subscribe({
      next: (reservations) => {
        this.state.set(reservations);
        this.loadStatus.set('loaded');
      },
      error: () => this.loadStatus.set('error'),
    });
  }

  /**
   * Annulation optimiste : l'écran change tout de suite, puis on revient en arrière si le serveur refuse.
   * Renvoie l'Observable pour que le composant puisse afficher un message (succès ou erreur).
   */
  cancel(id: string): Observable<ReservationResponse> {
    const previous = this.state();   // la photo d'avant, pour le rollback

    this.state.update((reservations) =>
      reservations.map((r) => (r.id === id ? { ...r, status: 'CANCELLED' as const } : r)),
    );

    return this.reservationService.cancelReservation(id).pipe(
      tap({ error: () => this.state.set(previous) }),
    );
  }

  clear(): void {
    this.state.set([]);
    this.loadStatus.set('idle');
  }
}
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { exhaustMap, filter, map, Observable, take, timeout, timer } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { MyReservation, ReservationStatus } from '../models/reservation';


export interface ReservationResponse {
  id: string;
  eventId: string;
  seats: number;
  status: ReservationStatus;
  createdAt: string;
}


@Injectable({ providedIn: 'root' })
export class ReservationService {

  static readonly POLL_INTERVAL_MS = 1000;
  static readonly RESULT_TIMEOUT_MS = 15000;

  private readonly http = inject(HttpClient);

  // paymentMethod : un jeton pm_… (jamais un numéro de carte)
  createReservation(eventId: string, seats: number, paymentMethod: string): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(
      `${API_BASE_URL}/reservations?eventId=${eventId}`,
      { seats, paymentMethod }
    );
  }

  /**
   * Le paiement passe par la saga Kafka : il est ASYNCHRONE. On redemande l'état de la réservation
   * toutes les secondes jusqu'à ce qu'elle ne soit plus "en attente" (confirmée, ou annulée avec un motif).
   */
  waitForPaymentResult(reservationId: string): Observable<MyReservation> {
    return timer(0, ReservationService.POLL_INTERVAL_MS).pipe(
      // exhaustMap : on ne relance pas de requête tant que la précédente n'a pas répondu
      exhaustMap(() => this.http.get<MyReservation[]>(`${API_BASE_URL}/reservations/me`)),
      map((reservations) => reservations.find((r) => r.id === reservationId)),
      filter((r): r is MyReservation => r !== undefined && r.status !== 'PENDING'),
      take(1),                                          // le premier résultat définitif suffit : on arrête
      timeout(ReservationService.RESULT_TIMEOUT_MS),    // au-delà, on n'attend plus (le résultat viendra dans "Mes réservations")
    );
  }
}

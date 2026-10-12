import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ReservationStatus } from '../models/reservation';


export interface ReservationResponse {
  id: string;
  eventId: string;
  seats: number;
  status: ReservationStatus;
  createdAt: string;
}


@Injectable({ providedIn: 'root' })
export class ReservationService {

  private readonly http = inject(HttpClient);

  // paymentMethod : un jeton pm_… (jamais un numéro de carte)
  createReservation(eventId: string, seats: number, paymentMethod: string): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(
      `${API_BASE_URL}/reservations?eventId=${eventId}`,
      { seats, paymentMethod }
    );
  }

  // Le backend vérifie que la réservation appartient bien à l'utilisateur du jeton (sinon 403)
  cancelReservation(id: string): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${API_BASE_URL}/reservations/${id}/cancel`, null);
  }
}

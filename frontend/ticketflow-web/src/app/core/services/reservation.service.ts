import { HttpClient } from '@angular/common/http';
import { Injectable, Service } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';


export interface ReservationResponse {
  id: string;
  eventId: string;
  seats: number;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
}


@Injectable({ providedIn: 'root' })
export class ReservationService  {

     constructor(private http: HttpClient) {}

  createReservation(eventId: string, seats: number) {
    return this.http.post<ReservationResponse>(
      `${API_BASE_URL}/reservations?eventId=${eventId}`,
      { seats }
    );
  }


}

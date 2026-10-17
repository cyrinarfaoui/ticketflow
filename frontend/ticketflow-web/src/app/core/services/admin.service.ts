import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { exhaustMap, interval, Observable, startWith, switchMap, takeWhile } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AdminReservation } from '../models/reservation';
import { CancellationJob } from '../models/cancellation';
import { CreateEventPayload, EventResponse } from './event.service';

/**
 * Actions de l'administrateur (écritures).
 * Les lectures du dashboard passent par httpResource, directement dans le composant.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly http = inject(HttpClient);

  cancelReservation(id: string): Observable<AdminReservation> {
    return this.http.post<AdminReservation>(`${API_BASE_URL}/reservations/${id}/cancel`, null);
  }

  closeEvent(id: string): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${API_BASE_URL}/events/${id}/close`, null);
  }

  // Traitement long côté serveur : 202 Accepted tout de suite, puis on suit l'avancement
  cancelEvent(id: string): Observable<CancellationJob> {
    return this.http.post<CancellationJob>(`${API_BASE_URL}/events/${id}/cancel`, null);
  }

  cancellationStatus(id: string): Observable<CancellationJob> {
    return this.http.get<CancellationJob>(`${API_BASE_URL}/events/${id}/cancellation`);
  }
    updateEvent(id: string, payload: CreateEventPayload): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${API_BASE_URL}/events/${id}`, payload);
  }

  deleteEvent(id: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/events/${id}`);
  }

    // Lance l'annulation (202), puis suit l'avancement chaque seconde jusqu'à DONE ou FAILED
  cancelEventAndWait(id: string): Observable<CancellationJob> {
    return this.cancelEvent(id).pipe(
      switchMap(() => interval(1000).pipe(
        startWith(0),
        exhaustMap(() => this.cancellationStatus(id)),
        takeWhile((job) => job.status === 'RUNNING', true),
      )),
    );
  }
}

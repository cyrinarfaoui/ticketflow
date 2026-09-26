import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams  } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';
import { Observable, tap } from 'rxjs';

export interface EventResponse {
  id: string;
  title: string;
  description: string;
  location: string;
  date: string;
  totalSeats: number;
  availableSeats: number;
}

@Injectable({ providedIn: 'root' })
export class EventService {

    
  private readonly events = signal<EventResponse[]>([]);
  readonly events$ = this.events.asReadonly();

  constructor(private http: HttpClient) {}



  loadEvents(): void {
    
    this.http.get<EventResponse[]>(`${API_BASE_URL}/events`).subscribe({
      next: (data) => this.events.set(data),
      error: (err) => console.error('Erreur lors du chargement des événements', err)
    });
  }


   search(title: string, location: string, availableOnly: boolean): Observable<EventResponse[]> {
    let params = new HttpParams();
    if (title) params = params.set('title', title);
    if (location) params = params.set('location', location);
    if (availableOnly) params = params.set('availableOnly', String(availableOnly));

    return this.http.get<EventResponse[]>(`${API_BASE_URL}/events`, { params }).pipe(
      tap((data) => this.events.set(data))
    );
  }



   createEvent(payload: { title: string; description: string; location: string; date: string; totalSeats: number }) {
  return this.http.post<EventResponse>(`${API_BASE_URL}/events`, payload);
   }

   
}
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';

export interface EventResponse {
  id: string;
  title: string;
  description: string;
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

   createEvent(payload: { title: string; description: string; date: string; totalSeats: number }) {
  return this.http.post<EventResponse>(`${API_BASE_URL}/events`, payload);
   }
}
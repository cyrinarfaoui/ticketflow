import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { EventCategory } from '../models/event-category';

export interface EventResponse {
  id: string;
  title: string;
  description: string;
  location: string;
  category: EventCategory;
  date: string;
  totalSeats: number;
  availableSeats: number;
}

// Filtres de recherche du catalogue (tous facultatifs ; dates au format AAAA-MM-JJ)
export interface EventSearchFilters {
  title?: string;
  location?: string;
  category?: EventCategory | '';
  from?: string;
  to?: string;
  availableOnly?: boolean;
}

export interface CreateEventPayload {
  title: string;
  description: string;
  location: string;
  category: EventCategory;
  date: string;
  totalSeats: number;
}

@Injectable({ providedIn: 'root' })
export class EventService {

  private readonly events = signal<EventResponse[]>([]);
  readonly events$ = this.events.asReadonly();

  constructor(private http: HttpClient) {}

  // N'envoie que les filtres renseignés : un champ vide = pas de filtre
  search(filters: EventSearchFilters): Observable<EventResponse[]> {
    let params = new HttpParams();
    if (filters.title) params = params.set('title', filters.title);
    if (filters.location) params = params.set('location', filters.location);
    if (filters.category) params = params.set('category', filters.category);
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.availableOnly) params = params.set('availableOnly', 'true');

    return this.http.get<EventResponse[]>(`${API_BASE_URL}/events`, { params }).pipe(
      tap((data) => this.events.set(data)),
    );
  }

  createEvent(payload: CreateEventPayload): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${API_BASE_URL}/events`, payload);
  }
}

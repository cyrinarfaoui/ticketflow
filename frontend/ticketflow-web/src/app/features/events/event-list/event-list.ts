import { Component, inject, OnInit, signal } from '@angular/core';
import { EventService } from '../../../core/services/event.service';
import { RouterLink } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  imports: [RouterLink],
  selector: 'app-event-list',
  styleUrl: './event-list.scss',
  templateUrl: './event-list.html',
})
export class EventList implements OnInit {


  protected authService = inject(AuthService);

  readonly seatsToBook = signal<Record<string, number>>({});
  readonly errorMessage = signal<string | null>(null);

  constructor(
  
    protected eventService: EventService,
    private reservationService: ReservationService
  ) {}

  ngOnInit(): void {
    this.eventService.loadEvents();
  }

  getSeats(eventId: string): number {
    return this.seatsToBook()[eventId] ?? 1;
  }

  setSeats(eventId: string, value: string): void {
    this.seatsToBook.update((current) => ({ ...current, [eventId]: Number(value) }));
  }

  reserve(eventId: string): void {
    this.errorMessage.set(null);
    const seats = this.getSeats(eventId);

    this.reservationService.createReservation(eventId, seats).subscribe({
      next: () => this.eventService.loadEvents(),
      error: (err) => {
        const message = err.error?.error ?? 'Une erreur est survenue lors de la réservation.';
        this.errorMessage.set(message);
      }
    });
  }

}

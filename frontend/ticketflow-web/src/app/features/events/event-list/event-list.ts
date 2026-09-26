import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { EventService } from '../../../core/services/event.service';
import { RouterLink } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { combineLatest, debounceTime, distinctUntilChanged, startWith, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  imports: [RouterLink, ReactiveFormsModule],
  selector: 'app-event-list',
  styleUrl: './event-list.scss',
  templateUrl: './event-list.html',
})
export class EventList implements OnInit {


  protected authService = inject(AuthService);
  private destroyRef = inject(DestroyRef);

  readonly seatsToBook = signal<Record<string, number>>({});
  readonly errorMessage = signal<string | null>(null);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly locationControl = new FormControl('', { nonNullable: true });
  readonly availableOnlyControl = new FormControl(false, { nonNullable: true });

   constructor(
    protected eventService: EventService,
    private reservationService: ReservationService
  ) {}

  ngOnInit(): void {
    this.eventService.loadEvents();
    combineLatest([
      this.searchControl.valueChanges.pipe(startWith('')),
      this.locationControl.valueChanges.pipe(startWith('')),
      this.availableOnlyControl.valueChanges.pipe(startWith(false))
    ]).pipe(
      debounceTime(300),
      distinctUntilChanged((a, b) => a[0] === b[0] && a[1] === b[1] && a[2] === b[2]),
      switchMap(([title, location, availableOnly]) => this.eventService.search(title, location, availableOnly)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
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

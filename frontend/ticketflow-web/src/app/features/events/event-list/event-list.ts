import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest, debounceTime, distinctUntilChanged, startWith, switchMap } from 'rxjs';
import { EventService } from '../../../core/services/event.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { EventCard } from '../event-card/event-card';

@Component({
  imports: [RouterLink, ReactiveFormsModule, EventCard],
  selector: 'app-event-list',
  styleUrl: './event-list.scss',
  templateUrl: './event-list.html',
})
export class EventList implements OnInit {

  protected authService = inject(AuthService);
  private destroyRef = inject(DestroyRef);

  readonly errorMessage = signal<string | null>(null);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly locationControl = new FormControl('', { nonNullable: true });
  readonly availableOnlyControl = new FormControl(false, { nonNullable: true });

  constructor(
    protected eventService: EventService,
    private reservationService: ReservationService,
  ) {}

  ngOnInit(): void {
    this.eventService.loadEvents();
    combineLatest([
      this.searchControl.valueChanges.pipe(startWith('')),
      this.locationControl.valueChanges.pipe(startWith('')),
      this.availableOnlyControl.valueChanges.pipe(startWith(false)),
    ]).pipe(
      debounceTime(300),
      distinctUntilChanged((a, b) => a[0] === b[0] && a[1] === b[1] && a[2] === b[2]),
      switchMap(([title, location, availableOnly]) => this.eventService.search(title, location, availableOnly)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  reserve(eventId: string, seats: number): void {
    this.errorMessage.set(null);

    this.reservationService.createReservation(eventId, seats).subscribe({
      next: () => this.eventService.loadEvents(),
      error: (err: HttpErrorResponse) => {
        // Les erreurs techniques (réseau, 401, 403, 5xx) sont affichées par la bannière globale (errorInterceptor).
        // Ici, on n'affiche que les erreurs métier renvoyées par l'API, par exemple "Pas assez de places".
        this.errorMessage.set(err.error?.error ?? null);
      },
    });
  }
}


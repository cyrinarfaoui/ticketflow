import { Component, computed, input, output, signal } from '@angular/core';
import { EventResponse } from '../../../core/services/event.service';
import { DatePipe } from '@angular/common';


// Les 3 états possibles d'un événement
type SeatsStatus = 'available' | 'few' | 'full';


@Component({
  imports: [DatePipe],
  selector: 'app-event-card',
  styleUrl: './event-card.scss',
  templateUrl: './event-card.html',
})
export class EventCard {

   // ENTRÉE : le parent nous donne l'événement à afficher
readonly event = input.required<EventResponse>();

// SORTIE : on prévient le parent "je veux réserver N places"
readonly reserve = output<number>();

  // ÉTAT LOCAL : le nombre de places choisi dans CETTE carte (1 par défaut)
  readonly seats = signal(1);

   // CALCULÉ : pourcentage de places restantes (0 à 100)
  readonly fillPercent = computed(() => {
    const e = this.event();
    return Math.round((e.availableSeats / e.totalSeats) * 100);
  });

  // CALCULÉ : l'état de l'événement, déduit du pourcentage
  readonly status = computed<SeatsStatus>(() => {
    if (this.event().availableSeats === 0) {
      return 'full';
    }
    if (this.fillPercent() <= 10) {
      return 'few';
    }
    return 'available';
  });

  onSeatsChange(value: string): void {
    this.seats.set(Number(value));
  }

  onReserve(): void {
    this.reserve.emit(this.seats());
  }


}

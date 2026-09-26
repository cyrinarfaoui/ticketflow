import { Component, input, output, signal } from '@angular/core';
import { EventResponse } from '../../../core/services/event.service';

@Component({
  imports: [],
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

  onSeatsChange(value: string): void {
    this.seats.set(Number(value));
  }

  onReserve(): void {
    this.reserve.emit(this.seats());
  }


}

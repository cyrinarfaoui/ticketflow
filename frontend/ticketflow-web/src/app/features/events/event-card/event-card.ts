import { Component, computed, input, output, signal } from '@angular/core';
import { EventResponse } from '../../../core/services/event.service';
import { categoryLabel } from '../../../core/models/event-category';
import { DatePipe } from '@angular/common';
import { formatPrice } from '../../../core/models/payment';


// Les états possibles d'un événement
type SeatsStatus = 'available' | 'few' | 'full' | 'closed' | 'cancelled';


@Component({
  imports: [DatePipe],
  selector: 'app-event-card',
  styleUrl: './event-card.scss',
  templateUrl: './event-card.html',
})
export class EventCard {

// OPTION : afficher ou non la zone de réservation (true par défaut)
readonly bookable = input(true);
   // ENTRÉE : le parent nous donne l'événement à afficher
readonly event = input.required<EventResponse>();

// SORTIE : on prévient le parent "je veux réserver N places" (le paiement se fait ensuite, dans la fenêtre de paiement)
readonly reserve = output<number>();

  // ÉTAT LOCAL : le nombre de places choisi dans CETTE carte (1 par défaut)
  readonly seats = signal(1);

  // Prix d'une place et total à payer pour les places choisies
  readonly price = computed(() => formatPrice(this.event().priceCents, this.event().currency));
  readonly total = computed(() => formatPrice(this.event().priceCents * this.seats(), this.event().currency));

  // L'attribut max="…" n'empêche PAS de taper 60 : on vérifie nous-mêmes, AVANT d'ouvrir le paiement
  readonly seatsError = computed(() => {
    const seats = this.seats();
    const available = this.event().availableSeats;
    if (!Number.isInteger(seats) || seats < 1) {
      return 'Choisissez au moins 1 place.';
    }
    if (seats > available) {
      return `Il ne reste que ${available} place(s).`;
    }
    return null;
  });

  // Libellé lisible de la catégorie : CONCERT -> "Concert"
  readonly category = computed(() => categoryLabel(this.event().category));

   // CALCULÉ : pourcentage de places restantes (0 à 100)
  readonly fillPercent = computed(() => {
    const e = this.event();
    return Math.round((e.availableSeats / e.totalSeats) * 100);
  });

  // CALCULÉ : l'état de l'événement, déduit du pourcentage
  readonly status = computed<SeatsStatus>(() => {
    
    if (this.event().status === 'CANCELLED') {
      return 'cancelled';
    }

    if (this.event().status === 'CLOSED') {
      return 'closed';
    }
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
    if (this.seatsError()) {
      return;   // nombre de places invalide : pas de fenêtre de paiement
    }
    this.reserve.emit(this.seats());
  }


}

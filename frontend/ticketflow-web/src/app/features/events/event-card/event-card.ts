import { Component, computed, input, output, signal } from '@angular/core';
import { EventResponse } from '../../../core/services/event.service';
import { categoryLabel } from '../../../core/models/event-category';
import { DatePipe } from '@angular/common';
import { formatPrice, ReserveRequest, TEST_CARDS } from '../../../core/models/payment';


// Les états possibles d'un événement
type SeatsStatus = 'available' | 'few' | 'full' | 'closed';


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

// SORTIE : on prévient le parent "je veux réserver N places avec ce moyen de paiement"
readonly reserve = output<ReserveRequest>();

  // Cartes de test proposées (simulation du formulaire de paiement Stripe)
  readonly testCards = TEST_CARDS;
  readonly paymentMethod = signal(TEST_CARDS[0].token);

  // ÉTAT LOCAL : le nombre de places choisi dans CETTE carte (1 par défaut)
  readonly seats = signal(1);

  // Prix d'une place et total à payer pour les places choisies
  readonly price = computed(() => formatPrice(this.event().priceCents, this.event().currency));
  readonly total = computed(() => formatPrice(this.event().priceCents * this.seats(), this.event().currency));

  // Libellé lisible de la catégorie : CONCERT -> "Concert"
  readonly category = computed(() => categoryLabel(this.event().category));

   // CALCULÉ : pourcentage de places restantes (0 à 100)
  readonly fillPercent = computed(() => {
    const e = this.event();
    return Math.round((e.availableSeats / e.totalSeats) * 100);
  });

  // CALCULÉ : l'état de l'événement, déduit du pourcentage
  readonly status = computed<SeatsStatus>(() => {
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

  onPaymentMethodChange(value: string): void {
    this.paymentMethod.set(value);
  }

  onReserve(): void {
    this.reserve.emit({ seats: this.seats(), paymentMethod: this.paymentMethod() });
  }


}

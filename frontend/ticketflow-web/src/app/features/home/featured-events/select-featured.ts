import { EventResponse } from '../../../core/services/event.service';

/**
 * Sélectionne les événements "À la une" : les prochains événements à venir
 * qui ont encore des places, du plus proche au plus lointain.
 */
export function selectFeatured(
  events: readonly EventResponse[],
  now: Date,
  limit = 3,
): EventResponse[] {
  return events
    .filter((event) => startTime(event) > now.getTime())
    .filter((event) => event.availableSeats > 0)
    .sort((a, b) => startTime(a) - startTime(b))
    .slice(0, limit);
}

// Heure de début d'un événement, en millisecondes (facile à comparer)
function startTime(event: EventResponse): number {
  return new Date(event.date).getTime();
}
// Catégories d'événements : mêmes valeurs que l'enum Java EventCategory (event-service)
export type EventCategory = 'CONCERT' | 'THEATRE' | 'CONFERENCE' | 'SPORT' | 'FESTIVAL' | 'AUTRE';

// Libellés affichés à l'utilisateur, dans l'ordre des listes déroulantes
export const EVENT_CATEGORIES: readonly { value: EventCategory; label: string }[] = [
  { value: 'CONCERT', label: 'Concert' },
  { value: 'THEATRE', label: 'Théâtre' },
  { value: 'CONFERENCE', label: 'Conférence' },
  { value: 'SPORT', label: 'Sport' },
  { value: 'FESTIVAL', label: 'Festival' },
  { value: 'AUTRE', label: 'Autre' },
];

export function categoryLabel(category: EventCategory): string {
  return EVENT_CATEGORIES.find((c) => c.value === category)?.label ?? category;
}

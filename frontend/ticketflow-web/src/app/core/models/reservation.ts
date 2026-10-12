// Modèles du dashboard admin, alignés sur les records Java de event-service

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';

export const RESERVATION_STATUSES: readonly { value: ReservationStatus; label: string }[] = [
  { value: 'PENDING', label: 'En attente' },
  { value: 'CONFIRMED', label: 'Confirmée' },
  { value: 'CANCELLED', label: 'Annulée' },
  { value: 'EXPIRED', label: 'Expirée' },
];

export function reservationStatusLabel(status: ReservationStatus): string {
  return RESERVATION_STATUSES.find((s) => s.value === status)?.label ?? status;
}

// ReservationResponse.java
export interface AdminReservation {
  id: string;
  eventId: string;
  seats: number;
  status: ReservationStatus;
  createdAt: string;
}

// MyReservationResponse.java : une ligne de "Mes réservations", avec l'événement
export interface MyReservation {
  id: string;
  eventId: string;
  eventTitle: string;
  eventLocation: string | null;
  eventDate: string | null;
  seats: number;
  status: ReservationStatus;
  createdAt: string;
  paymentFailureCode: string | null;   // motif du refus de paiement (codes Stripe), sinon null
}

// PageResponse.java : une page de résultats + de quoi afficher "page 2 sur 5"
export interface Page<T> {
  content: T[];
  page: number;          // numéro de page, commence à 0 (convention Spring)
  size: number;
  totalElements: number;
  totalPages: number;
}

// ReservationStatsResponse.java
export interface ReservationStats {
  createdToday: number;
  total: number;
  pending: number;
  confirmed: number;
  cancelled: number;
  expired: number;
  expirationRate: number; // entre 0 et 1
}

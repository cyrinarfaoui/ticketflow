package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.Reservation;

import java.time.Instant;

/**
 * Message publié sur reservation.created.
 * Version 2 : ajoute le montant, la devise et le jeton de moyen de paiement (lus par payment-service).
 * Les champs de la version 1 sont inchangés : notification-service continue de fonctionner sans modification.
 */
public record ReservationCreatedEvent(int version,
                                      String reservationId,
                                      String eventId,
                                      int seats,
                                      String status,
                                      long amountCents,
                                      String currency,
                                      String paymentMethod,
                                      Instant occurredAt) {

    public static ReservationCreatedEvent from(Reservation reservation, long amountCents, String paymentMethod) {
        return new ReservationCreatedEvent(
                2,
                reservation.getId().toString(),
                reservation.getEventId().toString(),
                reservation.getSeats(),
                reservation.getStatus().name(),
                amountCents,
                Event.CURRENCY,
                paymentMethod,
                Instant.now());
    }
}

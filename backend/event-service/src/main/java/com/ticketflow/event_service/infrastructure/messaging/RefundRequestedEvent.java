package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.domain.Reservation;

import java.time.Instant;

/**
 * Message publié sur refund.requested, lu par payment-service.
 * reason = le statut de la réservation au moment de la demande (EXPIRED, CANCELLED), pour la traçabilité.
 */
public record RefundRequestedEvent(String reservationId, String reason, Instant occurredAt) {

    public static RefundRequestedEvent from(Reservation reservation, Instant now) {
        return new RefundRequestedEvent(reservation.getId().toString(), reservation.getStatus().name(), now);
    }
}

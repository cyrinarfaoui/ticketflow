package com.ticketflow.payment_service.infrastructure.messaging;

import java.time.Instant;

/** Message reçu sur refund.requested (publié par event-service) : rembourser le paiement de cette réservation. */
public record RefundRequestedMessage(String reservationId, String reason, Instant occurredAt) {
}

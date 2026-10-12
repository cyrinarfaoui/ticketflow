package com.ticketflow.event_service.infrastructure.messaging;

import java.time.Instant;

/** Message reçu sur payment.completed / payment.failed (publié par payment-service). */
public record PaymentResultMessage(
        int version,
        String paymentId,
        String reservationId,
        String status,
        String declineCode,
        long amountCents,
        String currency,
        Instant occurredAt
) {

    boolean isSucceeded() {
        return "SUCCEEDED".equals(status);
    }
}

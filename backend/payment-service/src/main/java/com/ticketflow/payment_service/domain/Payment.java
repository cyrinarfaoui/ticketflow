package com.ticketflow.payment_service.domain;

import java.time.Instant;
import java.util.UUID;

/** Un paiement effectué : ce qui a été demandé, ce que la banque (ou nos règles) a répondu, et quand. */
public record Payment(UUID id, UUID reservationId, Money amount, String paymentMethod,
                      PaymentResult result, Instant createdAt) {

    public static Payment of(PaymentRequest request, PaymentResult result, Instant createdAt) {
        return new Payment(UUID.randomUUID(), request.reservationId(), request.amount(),
                request.paymentMethod(), result, createdAt);
    }
}

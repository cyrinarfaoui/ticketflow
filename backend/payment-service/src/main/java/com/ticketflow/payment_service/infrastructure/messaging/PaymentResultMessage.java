package com.ticketflow.payment_service.infrastructure.messaging;

import com.ticketflow.payment_service.domain.Payment;

import java.time.Instant;

/**
 * Message publié sur payment.completed, payment.failed ou payment.refunded.
 * declineCode = le code Stripe du refus (insufficient_funds…), null si le paiement est accepté.
 */
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

    static PaymentResultMessage from(Payment payment, Instant now) {
        return new PaymentResultMessage(
                1,
                payment.id().toString(),
                payment.reservationId().toString(),
                payment.result().status().name(),
                payment.result().declineCode() == null ? null : payment.result().declineCode().code(),
                payment.amount().amountCents(),
                payment.amount().currency(),
                now);
    }
}

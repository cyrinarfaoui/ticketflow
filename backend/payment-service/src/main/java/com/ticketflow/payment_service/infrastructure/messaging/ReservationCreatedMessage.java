package com.ticketflow.payment_service.infrastructure.messaging;

import java.time.Instant;

/**
 * Message reçu sur le topic reservation.created (publié par event-service).
 * Version 2 : ajoute le montant, la devise et le jeton de moyen de paiement.
 * Les messages de version 1 (sans montant) sont ignorés : ils datent d'avant le paiement.
 */
public record ReservationCreatedMessage(
        int version,
        String reservationId,
        String eventId,
        int seats,
        String status,
        Long amountCents,
        String currency,
        String paymentMethod,
        Instant occurredAt
) {

    boolean isPayable() {
        return amountCents != null && currency != null && paymentMethod != null;
    }
}

package com.ticketflow.event_service.domain;

/** Ce qu'on dit au client quand son paiement a été remboursé. */
public record RefundNotice(String email, String eventTitle, int seats, long amountCents, String currency) {
}

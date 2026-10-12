package com.ticketflow.payment_service.domain;

/** Motifs de refus, avec les codes utilisés par Stripe. */
public enum DeclineCode {

    // Règles du commerçant (vérifiées avant d'appeler la banque)
    INVALID_AMOUNT("invalid_amount"),
    AMOUNT_TOO_LARGE("amount_too_large"),

    // Réponses de la banque (via le prestataire)
    CARD_DECLINED("card_declined"),
    INSUFFICIENT_FUNDS("insufficient_funds"),
    EXPIRED_CARD("expired_card"),
    INVALID_PAYMENT_METHOD("invalid_payment_method");

    private final String code;

    DeclineCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
package com.ticketflow.payment_service.domain;

import java.util.Optional;

/**
 * Règles du COMMERÇANT, vérifiées avant de solliciter la banque :
 * inutile de payer des frais de transaction pour une demande qu'on sait déjà refuser.
 */
public final class PaymentRules {

    // Plafond par transaction : 2 000,00 dans la devise du paiement
    static final long MAX_AMOUNT_CENTS = 200_000;

    private PaymentRules() {
        // classe utilitaire : pas d'instance
    }

    public static Optional<DeclineCode> check(Money amount) {
        if (amount.amountCents() == 0) {
            return Optional.of(DeclineCode.INVALID_AMOUNT);
        }
        if (amount.amountCents() > MAX_AMOUNT_CENTS) {
            return Optional.of(DeclineCode.AMOUNT_TOO_LARGE);
        }
        return Optional.empty();
    }
}
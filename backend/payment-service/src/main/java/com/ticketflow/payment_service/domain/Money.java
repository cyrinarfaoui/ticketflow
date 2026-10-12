package com.ticketflow.payment_service.domain;

/**
 * Un montant d'argent : des CENTIMES ENTIERS et une devise ISO (TND, EUR…).
 * Jamais de double pour de l'argent : 0.1 + 0.2 = 0.30000000000000004.
 */
public record Money(long amountCents, String currency) {

    public Money {
        if (amountCents < 0) {
            throw new IllegalArgumentException("Un montant ne peut pas être négatif");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("La devise doit être un code ISO de 3 lettres majuscules (ex. TND)");
        }
    }
}
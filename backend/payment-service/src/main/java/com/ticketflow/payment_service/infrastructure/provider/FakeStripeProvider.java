package com.ticketflow.payment_service.infrastructure.provider;

import com.ticketflow.payment_service.application.port.out.PaymentProvider;
import com.ticketflow.payment_service.domain.DeclineCode;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;
import org.springframework.stereotype.Component;

/**
 * Imite le mode test de Stripe : chaque jeton de test provoque une réponse précise de la banque.
 * Remplacé au ticket #5 par un vrai appel HTTP (WebClient) vers un prestataire.
 */
@Component
public class FakeStripeProvider implements PaymentProvider {

    @Override
    public PaymentResult charge(PaymentRequest request) {
        return switch (request.paymentMethod()) {
            case "pm_card_visa", "pm_card_mastercard" -> PaymentResult.succeeded();
            case "pm_card_chargeDeclined" -> PaymentResult.declined(DeclineCode.CARD_DECLINED);
            case "pm_card_insufficientFunds" -> PaymentResult.declined(DeclineCode.INSUFFICIENT_FUNDS);
            case "pm_card_expired" -> PaymentResult.declined(DeclineCode.EXPIRED_CARD);
            // Jeton inconnu : en cas de doute, on REFUSE (fail closed), jamais on n'accepte
            default -> PaymentResult.declined(DeclineCode.INVALID_PAYMENT_METHOD);
        };
    }
}
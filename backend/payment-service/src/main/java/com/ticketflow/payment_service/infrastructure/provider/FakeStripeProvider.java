package com.ticketflow.payment_service.infrastructure.provider;

import com.ticketflow.payment_service.application.port.out.PaymentProvider;
import com.ticketflow.payment_service.domain.DeclineCode;
import com.ticketflow.payment_service.domain.Payment;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Imite le mode test de Stripe : chaque jeton de test provoque une réponse précise de la banque.
 * Remplacé au ticket #5 par un vrai appel HTTP (WebClient) vers un prestataire.
 */
@Component
public class FakeStripeProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(FakeStripeProvider.class);

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

    @Override
    public void refund(Payment payment) {
        // En mode test, Stripe accepte toujours le remboursement d'un paiement réussi.
        // Le vrai appel enverrait l'en-tête "Idempotency-Key: <id du paiement>" : un rejeu ne rembourse pas deux fois.
        log.info("Remboursement simulé de {} {} (réservation {}, clé d'idempotence {})",
                payment.amount().amountCents(), payment.amount().currency(), payment.reservationId(), payment.id());
    }
}
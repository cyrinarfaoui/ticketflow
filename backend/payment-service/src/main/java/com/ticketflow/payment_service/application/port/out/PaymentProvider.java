package com.ticketflow.payment_service.application.port.out;

import com.ticketflow.payment_service.domain.Payment;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;

/**
 * Le prestataire de paiement (Stripe, PayPal, banque…), vu par le métier.
 * Le métier ne connaît que cette interface : changer de prestataire = changer d'adaptateur.
 */
public interface PaymentProvider {

    PaymentResult charge(PaymentRequest request);

    // Rend au client l'argent d'un paiement accepté (chez Stripe : l'API refunds).
    // Clé d'idempotence = id du paiement : appelé deux fois, le prestataire ne rembourse qu'une fois.
    void refund(Payment payment);
}
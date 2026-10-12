package com.ticketflow.payment_service.application.port.out;

import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;

/**
 * Le prestataire de paiement (Stripe, PayPal, banque…), vu par le métier.
 * Le métier ne connaît que cette interface : changer de prestataire = changer d'adaptateur.
 */
public interface PaymentProvider {

    PaymentResult charge(PaymentRequest request);
}
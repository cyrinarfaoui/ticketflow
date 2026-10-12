package com.ticketflow.payment_service.application.port.out;

import com.ticketflow.payment_service.domain.Payment;

/** Annonce le résultat d'un paiement aux autres services (payment.completed / payment.failed). */
public interface PaymentEventPublisher {

    void publish(Payment payment);
}

package com.ticketflow.payment_service.application.port.out;

import com.ticketflow.payment_service.domain.Payment;

/** Annonce le résultat d'un paiement aux autres services (payment.completed / payment.failed / payment.refunded). */
public interface PaymentEventPublisher {

    void publish(Payment payment);

    /** Le paiement a été remboursé : event-service prévient le client. */
    void publishRefunded(Payment payment);
}

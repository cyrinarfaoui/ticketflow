package com.ticketflow.payment_service.infrastructure.messaging;

import com.ticketflow.payment_service.application.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** event-service demande un remboursement : le client a payé mais n'a pas (ou plus) de place. */
@Component
public class KafkaRefundRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaRefundRequestedListener.class);

    private final PaymentService paymentService;

    public KafkaRefundRequestedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Le type par défaut (application.properties) est ReservationCreatedMessage : on le remplace pour ce topic
    @KafkaListener(topics = "refund.requested",
            properties = "spring.json.value.default.type=com.ticketflow.payment_service.infrastructure.messaging.RefundRequestedMessage")
    public void onRefundRequested(RefundRequestedMessage message) {
        log.info("Remboursement demandé pour la réservation {} (réservation {})", message.reservationId(), message.reason());
        paymentService.refund(UUID.fromString(message.reservationId()));
    }
}

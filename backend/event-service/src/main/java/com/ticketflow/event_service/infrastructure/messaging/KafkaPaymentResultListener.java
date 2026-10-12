package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.application.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fin de la saga côté event-service : le paiement a réussi (on confirme) ou échoué (on compense).
 * Les deux traitements sont idempotents : Kafka peut livrer un message plusieurs fois.
 */
@Component
public class KafkaPaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentResultListener.class);

    private final ReservationService reservationService;

    public KafkaPaymentResultListener(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @KafkaListener(topics = {"payment.completed", "payment.failed"})
    public void onPaymentResult(PaymentResultMessage message) {
        UUID reservationId = UUID.fromString(message.reservationId());
        if (message.isSucceeded()) {
            log.info("Paiement accepté pour la réservation {} : confirmation", reservationId);
            reservationService.confirmPayment(reservationId);
        } else {
            log.info("Paiement refusé pour la réservation {} ({}) : compensation", reservationId, message.declineCode());
            reservationService.rejectPayment(reservationId, message.declineCode());
        }
    }
}

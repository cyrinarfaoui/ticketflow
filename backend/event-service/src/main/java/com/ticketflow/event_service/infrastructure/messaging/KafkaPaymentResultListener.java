package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.application.ClientMailService;
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
    private final ClientMailService clientMailService;

    public KafkaPaymentResultListener(ReservationService reservationService, ClientMailService clientMailService) {
        this.reservationService = reservationService;
        this.clientMailService = clientMailService;
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

    // Le remboursement demandé (refund.requested) a été effectué par payment-service : on prévient le client
    @KafkaListener(topics = "payment.refunded")
    public void onRefunded(PaymentResultMessage message) {
        log.info("Paiement remboursé pour la réservation {}", message.reservationId());
        clientMailService.notifyRefund(UUID.fromString(message.reservationId()), message.amountCents(), message.currency());
    }
}

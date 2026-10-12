package com.ticketflow.payment_service.infrastructure.messaging;

import com.ticketflow.payment_service.application.PaymentService;
import com.ticketflow.payment_service.domain.Money;
import com.ticketflow.payment_service.domain.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Point d'entrée de la saga côté paiement : une réservation vient d'être créée, on tente de la payer. */
@Component
public class KafkaReservationCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaReservationCreatedListener.class);

    private final PaymentService paymentService;

    public KafkaReservationCreatedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "reservation.created")
    public void onReservationCreated(ReservationCreatedMessage message) {
        if (!message.isPayable()) {
            log.warn("Réservation {} ignorée : message sans montant (version {}, antérieure au paiement)",
                    message.reservationId(), message.version());
            return;
        }
        paymentService.process(new PaymentRequest(
                UUID.fromString(message.reservationId()),
                new Money(message.amountCents(), message.currency()),
                message.paymentMethod()));
    }
}

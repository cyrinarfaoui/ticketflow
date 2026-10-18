package com.ticketflow.payment_service.infrastructure.messaging;

import com.ticketflow.payment_service.application.port.out.PaymentEventPublisher;
import com.ticketflow.payment_service.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    static final String TOPIC_COMPLETED = "payment.completed";
    static final String TOPIC_FAILED = "payment.failed";
    static final String TOPIC_REFUNDED = "payment.refunded";

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaTemplate<String, PaymentResultMessage> kafkaTemplate;
    private final Clock clock;

    public KafkaPaymentEventPublisher(KafkaTemplate<String, PaymentResultMessage> kafkaTemplate, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Override
    public void publish(Payment payment) {
        send(payment.result().isSucceeded() ? TOPIC_COMPLETED : TOPIC_FAILED, payment);
    }

    @Override
    public void publishRefunded(Payment payment) {
        send(TOPIC_REFUNDED, payment);   // même format de message, statut REFUNDED
    }

    private void send(String topic, Payment payment) {
        PaymentResultMessage message = PaymentResultMessage.from(payment, Instant.now(clock));

        // Clé = id de réservation : tous les messages d'une même réservation vont dans la même partition,
        // donc sont lus dans l'ordre
        kafkaTemplate.send(topic, message.reservationId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Échec d'envoi {} pour la réservation {}", topic, message.reservationId(), ex);
                    } else {
                        log.info("{} envoyé pour la réservation {} ({})", topic, message.reservationId(),
                                message.declineCode() == null ? message.status() : message.declineCode());
                    }
                });
    }
}

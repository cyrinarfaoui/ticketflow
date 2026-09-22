package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaReservationEventPublisher implements ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaReservationEventPublisher.class);
    private static final String TOPIC = "reservation.created";

    private final KafkaTemplate<String, ReservationCreatedEvent> kafkaTemplate;

    public KafkaReservationEventPublisher(KafkaTemplate<String, ReservationCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishCreated(Reservation reservation) {
        ReservationCreatedEvent event = ReservationCreatedEvent.from(reservation);
        kafkaTemplate.send(TOPIC, event.reservationId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Échec d'envoi reservation.created pour {}", event.reservationId(), ex);
                    } else {
                        log.info("reservation.created envoyé pour {} (partition {}, offset {})",
                                event.reservationId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

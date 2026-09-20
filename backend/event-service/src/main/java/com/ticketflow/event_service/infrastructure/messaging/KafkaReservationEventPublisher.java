package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.domain.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaReservationEventPublisher implements ReservationEventPublisher {

    private static final String TOPIC = "reservation.created";

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void publishCreated(Reservation reservation) {
        String key = reservation.getId().toString();
        String message = "reservationId=%s;eventId=%s;seats=%d;status=%s".formatted(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getSeats(),
                reservation.getStatus());

        kafkaTemplate.send(TOPIC, key, message)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Échec de l'envoi de reservation.created pour {}", key, error);
                    } else {
                        log.info("reservation.created envoyé pour {} (partition {}, offset {})",
                                key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

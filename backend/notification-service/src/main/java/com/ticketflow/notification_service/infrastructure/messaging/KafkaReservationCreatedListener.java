package com.ticketflow.notification_service.infrastructure.messaging;

import com.ticketflow.notification_service.application.port.in.HandleReservationCreatedUseCase;
import com.ticketflow.notification_service.domain.ReservationCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaReservationCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaReservationCreatedListener.class);

    private final HandleReservationCreatedUseCase useCase;

    public KafkaReservationCreatedListener(HandleReservationCreatedUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(topics = "reservation.created")
    public void onMessage(ReservationCreatedMessage message) {
        log.info("Message reçu : {}", message);
        useCase.handle(toDomain(message));
    }

    private ReservationCreated toDomain(ReservationCreatedMessage message) {
        return new ReservationCreated(
                UUID.fromString(message.reservationId()),
                UUID.fromString(message.eventId()),
                message.seats(),
                message.status()
        );
    }
}

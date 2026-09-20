package com.ticketflow.notification_service.infrastructure.messaging;


import com.ticketflow.notification_service.application.port.in.HandleReservationCreatedUseCase;
import com.ticketflow.notification_service.domain.ReservationCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaReservationCreatedListener {

    private final HandleReservationCreatedUseCase useCase;

    @KafkaListener(topics = "reservation.created")
    public void onMessage(String message) {
        log.info("Message reçu sur reservation.created : {}", message);
        useCase.handle(parse(message));
    }

    // Format envoyé par event-service : reservationId=...;eventId=...;seats=...;status=...
    private ReservationCreated parse(String message) {
        Map<String, String> fields = Arrays.stream(message.split(";"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(p -> p[0], p -> p[1]));

        return new ReservationCreated(
                UUID.fromString(fields.get("reservationId")),
                UUID.fromString(fields.get("eventId")),
                Integer.parseInt(fields.get("seats")),
                fields.get("status"));
    }
}

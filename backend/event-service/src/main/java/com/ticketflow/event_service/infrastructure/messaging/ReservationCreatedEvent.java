package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.domain.Reservation;

import java.time.Instant;

public record ReservationCreatedEvent(int version,
                                      String reservationId,
                                      String eventId,
                                      int seats,
                                      String status,
                                      Instant occurredAt) {

    public static ReservationCreatedEvent from(Reservation reservation) {
        return new ReservationCreatedEvent(
                1,
                reservation.getId().toString(),
                reservation.getEventId().toString(),
                reservation.getSeats(),
                reservation.getStatus().name(),
                Instant.now());
    }
}

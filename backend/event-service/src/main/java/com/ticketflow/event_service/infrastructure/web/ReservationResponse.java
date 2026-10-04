package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(UUID id, UUID eventId, int seats, ReservationStatus status, Instant createdAt) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(), reservation.getEventId(), reservation.getSeats(),
                reservation.getStatus(), reservation.getCreatedAt()
        );
    }
}
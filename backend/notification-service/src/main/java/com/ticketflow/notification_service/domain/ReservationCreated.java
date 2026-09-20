package com.ticketflow.notification_service.domain;

import java.util.UUID;

public record ReservationCreated(
        UUID reservationId, UUID eventId, int seats, String status
) {
}

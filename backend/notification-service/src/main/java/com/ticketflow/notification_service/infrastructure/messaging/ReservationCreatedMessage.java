package com.ticketflow.notification_service.infrastructure.messaging;

import java.time.Instant;

public record ReservationCreatedMessage(
        int version,
        String reservationId,
        String eventId,
        int seats,
        String status,
        Instant occurredAt
) {
}

package com.ticketflow.notification_service.domain;

import java.util.UUID;

/** Ce que la notification a besoin de savoir sur un événement (lu dans event-service). */
public record EventDetails(UUID id, String title, int availableSeats) {
}

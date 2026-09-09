package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.domain.Event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        LocalDateTime date,
        int totalSeats,
        int availableSeats
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(), event.getTitle(), event.getDescription(),
                event.getDate(), event.getTotalSeats(), event.getAvailableSeats()
        );
    }
}

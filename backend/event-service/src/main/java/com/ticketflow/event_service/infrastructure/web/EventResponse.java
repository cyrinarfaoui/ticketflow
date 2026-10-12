package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String location,
        EventCategory category,
        LocalDateTime date,
        int totalSeats,
        int availableSeats,
        EventStatus status,
        long priceCents,
        String currency
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(), event.getTitle(), event.getDescription(), event.getLocation(), event.getCategory(),
                event.getDate(), event.getTotalSeats(), event.getAvailableSeats(), event.getStatus(),
                event.getPriceCents(), Event.CURRENCY
        );
    }
}

package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventStatus;

public class EventMapper {

    public static EventEntity toEntity(Event event) {
        return new EventEntity(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getCategory(),
                event.getDate(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getStatus()
        );
    }


    public static Event toDomain(EventEntity entity) {
        Event event = new Event(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getCategory(),
                entity.getDate(),
                entity.getTotalSeats()
        );
        if (entity.getAvailableSeats() < entity.getTotalSeats()) {
            event.book(entity.getTotalSeats() - entity.getAvailableSeats());
        }
        // Clôturer APRÈS avoir rejoué les places réservées : book() refuse un événement clôturé
        if (entity.getStatus() == EventStatus.CLOSED) {
            event.close();
        }
        return event;
    }


}

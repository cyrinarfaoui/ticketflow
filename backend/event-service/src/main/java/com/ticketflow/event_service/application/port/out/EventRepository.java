package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface EventRepository {

    Event save(Event event);
    Optional<Event> findById(UUID id);
    List<Event> findAll();
    List<Event> search(String title, String location, EventCategory category,
                       boolean availableOnly, LocalDateTime start, LocalDateTime end);
}

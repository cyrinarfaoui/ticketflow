package com.ticketflow.event_service.application;


import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.domain.Event;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository){
        this.eventRepository = eventRepository ;
    }

    public Event createEvent(String title, String description, String location, LocalDateTime date, int totalSeats) {
        Event event = new Event(UUID.randomUUID(), title, description, location, date, totalSeats);
        return eventRepository.save(event);
    }

    public List<Event> listEvents(String title, String location, boolean availableOnly) {
        return eventRepository.search(blankToNull(title), blankToNull(location), availableOnly);
    }

    // "?location=" (champ vide cote front) doit se comporter comme "pas de filtre"
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Event getEvent(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + id));
    }

    public Event bookSeats(UUID eventId, int seats) {
        Event event = getEvent(eventId);
        event.book(seats);
        return eventRepository.save(event);
    }

}

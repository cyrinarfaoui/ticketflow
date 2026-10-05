package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EventService {

    // "Pas de date de fin" : une borne très lointaine plutôt que null (voir EventJpaRepository)
    static final LocalDateTime NO_END = LocalDateTime.of(9999, 12, 31, 23, 59);

    private final EventRepository eventRepository;
    private final Clock clock;

    public EventService(EventRepository eventRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    public Event createEvent(String title, String description, String location, EventCategory category,
                             LocalDateTime date, int totalSeats) {
        Event event = new Event(UUID.randomUUID(), title, description, location, category, date, totalSeats);
        return eventRepository.save(event);
    }

    /**
     * Événements à venir correspondant aux filtres.
     * Le catalogue ne montre jamais d'événement passé, même si "from" est dans le passé.
     * "to" est inclus : jusqu'au 20 = jusqu'au 21 à 0h, exclu.
     */
    public List<Event> listEvents(String title, String location, EventCategory category,
                                  boolean availableOnly, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure ou égale à la date de début");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = (from == null || from.atStartOfDay().isBefore(now)) ? now : from.atStartOfDay();
        LocalDateTime end = (to == null) ? NO_END : to.plusDays(1).atStartOfDay();

        return eventRepository.search(blankToNull(title), blankToNull(location), category, availableOnly, start, end);
    }

    // "?location=" (champ vide côté front) doit se comporter comme "pas de filtre"
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Event getEvent(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + id));
    }



    @Transactional
    public Event bookSeats(UUID eventId, int seats) {
        Event event = getEventForUpdate(eventId);
        event.book(seats);
        return eventRepository.save(event);
    }

    @Transactional
    public Event closeEvent(UUID eventId) {
        Event event = getEventForUpdate(eventId);
        event.close();
        return eventRepository.save(event);
    }

    // Toute modification de l'événement passe par le verrou : sinon une écriture concurrente peut l'écraser
    private Event getEventForUpdate(UUID id) {
        return eventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + id));
    }
}
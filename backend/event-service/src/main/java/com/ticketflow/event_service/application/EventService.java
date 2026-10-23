package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventStatus;
import com.ticketflow.event_service.domain.EventCategory;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public EventService(EventRepository eventRepository, ReservationRepository reservationRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    public Event createEvent(String title, String description, String location, EventCategory category,
                             LocalDateTime date, int totalSeats, long priceCents) {
        Event event = new Event(UUID.randomUUID(), title, description, location, category, date, totalSeats, priceCents);
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

    // Back-office : un titre vide ("") veut dire "pas de filtre" (null pour la requête)
    public Page<Event> listForAdmin(String title, EventStatus status, Boolean soldOut, Pageable pageable) {
        String titleFilter = (title == null || title.isBlank()) ? null : title.trim();
        return eventRepository.searchForAdmin(titleFilter, status, soldOut, pageable);
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

    // Modifier : le service ORCHESTRE (verrou, sauvegarde) ; les RÈGLES sont dans Event.update()
    @Transactional
    public Event updateEvent(UUID id, String title, String description, String location, EventCategory category,
                             LocalDateTime date, int totalSeats, long priceCents) {
        Event event = eventRepository.findByIdForUpdate(id)   // verrou : pas de réservation pendant la modification
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + id));
        if (event.getDate().isBefore(LocalDateTime.now(clock))) {
            throw new IllegalStateException("Cet événement est passé : il ne peut plus être modifié");
        }
        event.update(title, description, location, category, date, totalSeats, priceCents);
        return eventRepository.save(event);
    }

    // Supprimer : règle entre DEUX agrégats (événement + réservations) → elle est ici, pas dans Event
    @Transactional
    public void deleteEvent(UUID id) {
        // Verrou : une réservation ne peut pas se glisser entre la vérification et la suppression
        if (eventRepository.findByIdForUpdate(id).isEmpty()) {
            throw new NoSuchElementException("Événement introuvable : " + id);
        }
        if (reservationRepository.existsByEventId(id)) {
            throw new IllegalStateException("Cet événement a des réservations : annulez-le plutôt que de le supprimer");
        }
        eventRepository.deleteById(id);
    }
}
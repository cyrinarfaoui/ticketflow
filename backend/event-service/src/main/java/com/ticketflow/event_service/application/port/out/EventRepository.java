package com.ticketflow.event_service.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.EventStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface EventRepository {

    Event save(Event event);
    Optional<Event> findById(UUID id);
    List<Event> findAll();
    List<Event> findAllById(Collection<UUID> ids);
    List<Event> search(String title, String location, EventCategory category,
                       boolean availableOnly, LocalDateTime start, LocalDateTime end);

    // Lit l'événement en le verrouillant jusqu'à la fin de la transaction (réservations concurrentes)
    Optional<Event> findByIdForUpdate(UUID id);
    // Back-office : tous les événements, filtrés et paginés
    Page<Event> searchForAdmin(String title, EventStatus status, Boolean soldOut, Pageable pageable);
    void deleteById(UUID id);
}

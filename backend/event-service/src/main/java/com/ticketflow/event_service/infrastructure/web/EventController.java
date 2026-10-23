package com.ticketflow.event_service.infrastructure.web;


import com.ticketflow.event_service.application.EventService;
import com.ticketflow.event_service.domain.EventCategory;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import com.ticketflow.event_service.domain.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ticketflow.event_service.application.CancellationJob;
import com.ticketflow.event_service.application.EventCancellationJobs;
import java.net.URI;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {


    private final EventService eventService;
    private final EventCancellationJobs cancellationJobs;

    public EventController(EventService eventService, EventCancellationJobs cancellationJobs) {
        this.eventService = eventService;
        this.cancellationJobs = cancellationJobs;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        var event = eventService.createEvent(
                request.title(), request.description(), request.location(), request.category(),
                request.date(), request.totalSeats(), request.priceCents()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
    }

    @GetMapping
    public List<EventResponse> listEvents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false, defaultValue = "false") boolean availableOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return eventService.listEvents(title, location, category, availableOnly, from, to)
                .stream().map(EventResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable UUID id) {
        return EventResponse.from(eventService.getEvent(id));
    }

    @PostMapping("/{id}/book")
    public EventResponse bookSeats(@PathVariable UUID id, @Valid @RequestBody BookSeatsRequest request) {
        return EventResponse.from(eventService.bookSeats(id, request.seats()));
    }

    // Back-office : liste paginée et filtrée (10 par page, triée par date). Réservé à l'admin (SecurityConfig).
    // Le chemin littéral "/admin" passe AVANT "/{id}" : Spring préfère toujours le chemin le plus précis.
    @GetMapping("/admin")
    public PageResponse<EventResponse> listForAdmin(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) EventStatus status,
            // "Complet" n'est pas un statut en base : c'est un événement sans place restante
            @RequestParam(required = false) Boolean soldOut,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(eventService.listForAdmin(title, status, soldOut, pageable).map(EventResponse::from));
    }

    @PostMapping("/{id}/close")
    public EventResponse closeEvent(@PathVariable UUID id) {
        return EventResponse.from(eventService.closeEvent(id));
    }

    // Annulation : traitement long → 202 Accepted tout de suite ; l'avancement se suit sur GET .../cancellation
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancellationJob> cancelEvent(@PathVariable UUID id) {
        return ResponseEntity.accepted()
                .location(URI.create("/events/" + id + "/cancellation"))
                .body(cancellationJobs.start(id));
    }

    @GetMapping("/{id}/cancellation")
    public ResponseEntity<CancellationJob> cancellationStatus(@PathVariable UUID id) {
        return ResponseEntity.of(cancellationJobs.find(id));   // Optional vide → 404 (aucune annulation lancée)
    }

    // Même contenu qu'à la création : on réutilise CreateEventRequest (et ses validations @NotBlank, @Positive…)
    @PutMapping("/{id}")
    public EventResponse updateEvent(@PathVariable UUID id, @Valid @RequestBody CreateEventRequest request) {
        return EventResponse.from(eventService.updateEvent(id, request.title(), request.description(),
                request.location(), request.category(), request.date(), request.totalSeats(), request.priceCents()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();   // 204 : supprimé, rien à renvoyer
    }

}

package com.ticketflow.event_service.infrastructure.web;


import com.ticketflow.event_service.application.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {


    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        var event = eventService.createEvent(
                request.title(), request.description(), request.date(), request.totalSeats()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
    }

    @GetMapping
    public List<EventResponse> listEvents() {
        return eventService.listEvents().stream().map(EventResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable UUID id) {
        return EventResponse.from(eventService.getEvent(id));
    }

    @PostMapping("/{id}/book")
    public EventResponse bookSeats(@PathVariable UUID id, @Valid @RequestBody BookSeatsRequest request) {
        return EventResponse.from(eventService.bookSeats(id, request.seats()));
    }
}

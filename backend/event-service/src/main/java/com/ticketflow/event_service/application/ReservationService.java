package com.ticketflow.event_service.application;


import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.Reservation;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReservationService {


    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(EventRepository eventRepository, ReservationRepository reservationRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation createReservation(UUID eventId, int seats) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + eventId));

        event.book(seats);
        eventRepository.save(event);

        Reservation reservation = new Reservation(UUID.randomUUID(), eventId, seats);
        return reservationRepository.save(reservation);
    }

}

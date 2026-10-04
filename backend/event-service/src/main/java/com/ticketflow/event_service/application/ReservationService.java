package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStats;
import com.ticketflow.event_service.domain.ReservationStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReservationService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher eventPublisher;
    private final Clock clock;

    public ReservationService(EventRepository eventRepository,
                              ReservationRepository reservationRepository,
                              ReservationEventPublisher eventPublisher,
                              Clock clock) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Reservation createReservation(UUID eventId, int seats) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + eventId));

        event.book(seats);
        eventRepository.save(event);

        Reservation reservation = new Reservation(UUID.randomUUID(), eventId, seats);
        Reservation saved = reservationRepository.save(reservation);

        eventPublisher.publishCreated(saved);

        return saved;
    }

    @Transactional
    public Reservation cancelReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Réservation introuvable : " + reservationId));

        reservation.cancel();
        reservationRepository.save(reservation);

        Event event = eventRepository.findById(reservation.getEventId())
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + reservation.getEventId()));
        event.release(reservation.getSeats());
        eventRepository.save(event);

        return reservation;
    }

    public Page<Reservation> listReservations(ReservationStatus status, Pageable pageable) {
        return reservationRepository.findAll(status, pageable);
    }

    public ReservationStats stats() {
        // Minuit aujourd'hui, dans le fuseau de l'horloge (Tunis), converti en Instant
        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        return new ReservationStats(
                reservationRepository.countCreatedSince(startOfDay),
                reservationRepository.countByStatus()
        );
    }
}
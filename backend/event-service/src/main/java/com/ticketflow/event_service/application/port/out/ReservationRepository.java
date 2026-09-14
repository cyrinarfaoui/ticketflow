package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.Reservation;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    Reservation save(Reservation reservation);
    Optional<Reservation> findById(UUID id);
}

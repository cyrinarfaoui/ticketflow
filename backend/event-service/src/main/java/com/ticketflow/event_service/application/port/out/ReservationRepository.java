package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    Reservation save(Reservation reservation);
    Optional<Reservation> findById(UUID id);
    Page<Reservation> findAll(ReservationStatus status, Pageable pageable);
    long countCreatedSince(Instant since);
    Map<ReservationStatus, Long> countByStatus();
}

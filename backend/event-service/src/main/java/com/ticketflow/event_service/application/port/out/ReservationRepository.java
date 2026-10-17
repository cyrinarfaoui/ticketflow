package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    Reservation save(Reservation reservation);
    void saveAll(List<Reservation> reservations);
    // Réservations encore valables (en attente ou confirmées) d'un événement
    List<Reservation> findActiveByEventId(UUID eventId);
    Optional<Reservation> findById(UUID id);
    Page<Reservation> findAll(ReservationStatus status, Pageable pageable);
    // Les réservations d'un utilisateur, les plus récentes d'abord
    List<Reservation> findByUserId(String userId);
    long countCreatedSince(Instant since);
    Map<ReservationStatus, Long> countByStatus();
}

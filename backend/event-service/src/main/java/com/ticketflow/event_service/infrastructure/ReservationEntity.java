package com.ticketflow.event_service.infrastructure;


import com.ticketflow.event_service.domain.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor
public class ReservationEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    // Identifiant Keycloak ("sub" du JWT) ; null pour les réservations créées avant le ticket #10
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(nullable = false)
    private int seats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Raccourcis gardés pour les tests existants (réservation sans propriétaire)
    public ReservationEntity(UUID id, UUID eventId, int seats, ReservationStatus status) {
        this(id, eventId, null, seats, status, Instant.now());
    }

    public ReservationEntity(UUID id, UUID eventId, int seats, ReservationStatus status, Instant createdAt) {
        this(id, eventId, null, seats, status, createdAt);
    }

    public ReservationEntity(UUID id, UUID eventId, String userId, int seats, ReservationStatus status, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.seats = seats;
        this.status = status;
        this.createdAt = createdAt;
    }
}

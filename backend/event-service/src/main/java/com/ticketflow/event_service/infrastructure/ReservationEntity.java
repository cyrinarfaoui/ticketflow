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

    @Column(nullable = false)
    private int seats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Constructeur conservé pour ne rien casser là où il est déjà utilisé (ex. ReservationMapper)
    public ReservationEntity(UUID id, UUID eventId, int seats, ReservationStatus status) {
        this(id, eventId, seats, status, Instant.now());
    }

    // Nouveau constructeur : utilisé par le batch pour changer le statut sans perdre la date de création d'origine
    public ReservationEntity(UUID id, UUID eventId, int seats, ReservationStatus status, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.seats = seats;
        this.status = status;
        this.createdAt = createdAt;
    }
}

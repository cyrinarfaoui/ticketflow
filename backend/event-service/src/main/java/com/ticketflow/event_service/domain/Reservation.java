package com.ticketflow.event_service.domain;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Reservation {

    private final UUID id;
    private final UUID eventId;
    private final int seats;
    private ReservationStatus status;

    public Reservation(UUID id, UUID eventId, int seats) {
        if (seats <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être positif");
        }
        this.id = id;
        this.eventId = eventId;
        this.seats = seats;
        this.status = ReservationStatus.PENDING;
    }

    public void confirm() {
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de confirmer une réservation annulée");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cette réservation est déjà annulée");
        }
        this.status = ReservationStatus.CANCELLED;
    }
}

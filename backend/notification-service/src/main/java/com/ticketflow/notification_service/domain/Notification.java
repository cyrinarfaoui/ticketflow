package com.ticketflow.notification_service.domain;

import java.util.UUID;

/**
 * Notification PUBLIQUE diffusée en direct (SSE) : aucune donnée personnelle (ni utilisateur, ni n° de réservation).
 * eventTitle et availableSeats valent null si event-service n'a pas répondu (notification dégradée).
 */
public record Notification(UUID eventId, String eventTitle, int seats, Integer availableSeats, String message) {

    public static Notification enriched(ReservationCreated reservation, EventDetails event) {
        return new Notification(reservation.eventId(), event.title(), reservation.seats(), event.availableSeats(),
                "%d place(s) réservée(s) pour « %s » — il en reste %d"
                        .formatted(reservation.seats(), event.title(), event.availableSeats()));
    }

    public static Notification basic(ReservationCreated reservation) {
        return new Notification(reservation.eventId(), null, reservation.seats(), null,
                "%d place(s) viennent d'être réservée(s)".formatted(reservation.seats()));
    }
}
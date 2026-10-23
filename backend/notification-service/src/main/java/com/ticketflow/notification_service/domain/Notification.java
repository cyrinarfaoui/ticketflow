package com.ticketflow.notification_service.domain;

import java.util.UUID;

/**
 * Notification PUBLIQUE diffusée en direct (SSE) : aucune donnée personnelle (ni utilisateur, ni n° de réservation).
 * eventTitle et availableSeats valent null si event-service n'a pas répondu (notification dégradée).
 */
public record Notification(UUID eventId, String eventTitle, int seats, Integer availableSeats, String message) {

    public static Notification enriched(ReservationCreated reservation, EventDetails event) {
        return new Notification(reservation.eventId(), event.title(), reservation.seats(), event.availableSeats(),
                "« %s » : %s. %s".formatted(event.title(), justBooked(reservation.seats()), remaining(event.availableSeats())));
    }

    public static Notification basic(ReservationCreated reservation) {
        return new Notification(reservation.eventId(), null, reservation.seats(), null,
                capitalize(justBooked(reservation.seats())) + ".");
    }

    // Accord singulier / pluriel : « 1 place vient d'être réservée », « 4 places viennent d'être réservées »
    private static String justBooked(int seats) {
        return seats == 1 ? "1 place vient d'être réservée" : seats + " places viennent d'être réservées";
    }

    private static String remaining(int availableSeats) {
        if (availableSeats == 0) {
            return "L'événement est complet !";
        }
        return availableSeats == 1 ? "Plus qu'une place disponible !" : "Il reste " + availableSeats + " places.";
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

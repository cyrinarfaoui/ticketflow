package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.application.ReservationWithEvent;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une ligne de "Mes réservations" : la réservation + ce qu'il faut savoir de l'événement,
 * pour que le frontend n'ait pas à faire une requête par événement.
 */
public record MyReservationResponse(
        UUID id,
        UUID eventId,
        String eventTitle,
        String eventLocation,
        LocalDateTime eventDate,
        int seats,
        ReservationStatus status,
        Instant createdAt,
        // Code Stripe du refus de paiement (insufficient_funds…) si la saga a annulé la réservation
        String paymentFailureCode
) {
    public static MyReservationResponse from(ReservationWithEvent item) {
        Reservation reservation = item.reservation();
        Event event = item.event();
        return new MyReservationResponse(
                reservation.getId(),
                reservation.getEventId(),
                event != null ? event.getTitle() : "Événement supprimé",
                event != null ? event.getLocation() : null,
                event != null ? event.getDate() : null,
                reservation.getSeats(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getPaymentFailureCode()
        );
    }
}

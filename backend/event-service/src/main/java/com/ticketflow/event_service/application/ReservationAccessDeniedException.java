package com.ticketflow.event_service.application;

/**
 * L'utilisateur tente d'agir sur une réservation qui n'est pas la sienne.
 * Traduite en 403 Forbidden par le GlobalExceptionHandler.
 */
public class ReservationAccessDeniedException extends RuntimeException {

    public ReservationAccessDeniedException(String message) {
        super(message);
    }
}

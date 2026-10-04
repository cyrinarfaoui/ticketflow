package com.ticketflow.event_service.application;

import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.Reservation;

/**
 * Une réservation accompagnée de son événement, pour l'affichage "Mes réservations".
 * event peut être null si l'événement n'existe plus.
 */
public record ReservationWithEvent(Reservation reservation, Event event) {
}

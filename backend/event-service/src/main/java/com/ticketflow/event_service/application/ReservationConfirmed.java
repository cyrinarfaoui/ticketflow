package com.ticketflow.event_service.application;

import java.util.UUID;

/** Événement interne (Spring) : la réservation vient d'être confirmée. Déclenche l'envoi du billet APRÈS le commit. */
public record ReservationConfirmed(UUID reservationId) {
}

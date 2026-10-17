package com.ticketflow.event_service.domain;

public enum EventStatus {

    // Les réservations sont ouvertes
    OPEN,
    // L'administrateur a clôturé les réservations : plus aucune réservation possible
    CLOSED,
    // L'événement n'aura pas lieu (concert annulé) : définitif, toutes les réservations sont annulées
    CANCELLED

}

package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.Reservation;

public interface ReservationEventPublisher {

    /**
     * Annonce une nouvelle réservation, avec ce qu'il faut pour la payer
     * (montant en centimes et jeton de moyen de paiement).
     */
    void publishCreated(Reservation reservation, long amountCents, String paymentMethod);
}

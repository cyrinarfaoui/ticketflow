package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.CancellationNotice;

/**
 * Port de sortie : prévenir un client, sans dire COMMENT (e-mail, SMS, notification push…).
 * Appel LENT (réseau) et qui peut échouer : c'est pour ça que l'annulation prévient les clients en parallèle.
 */
public interface ClientNotifier {

    /** @throws RuntimeException si le client n'a pas pu être prévenu */
    void notifyEventCancelled(CancellationNotice notice);
}

package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.CancellationNotice;
import com.ticketflow.event_service.domain.RefundNotice;
import com.ticketflow.event_service.domain.TicketNotice;

/**
 * Port de sortie : prévenir un client, sans dire COMMENT (e-mail, SMS, notification push…).
 * Appel LENT (réseau) et qui peut échouer : c'est pour ça que l'annulation prévient les clients en parallèle.
 * Chaque méthode lève une RuntimeException si le client n'a pas pu être prévenu.
 */
public interface ClientNotifier {

    void notifyEventCancelled(CancellationNotice notice);

    /** Le billet, avec un QR code par place, après un paiement accepté. */
    void sendTicket(TicketNotice notice);

    void notifyRefund(RefundNotice notice);
}

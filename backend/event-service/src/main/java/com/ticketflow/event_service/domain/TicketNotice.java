package com.ticketflow.event_service.domain;

import java.time.LocalDateTime;
import java.util.List;

/** Le billet envoyé au client après paiement : un code (QR code) par place. */
public record TicketNotice(String email, String eventTitle, LocalDateTime eventDate, String location,
                           long amountCents, String currency, List<String> ticketCodes) {

    public int seats() {
        return ticketCodes.size();
    }
}

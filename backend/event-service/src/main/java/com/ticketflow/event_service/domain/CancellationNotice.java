package com.ticketflow.event_service.domain;

import java.time.LocalDateTime;

/** Ce qu'on doit dire à UN client quand son événement est annulé. */
public record CancellationNotice(String email, String eventTitle, LocalDateTime eventDate, int seats) {
}

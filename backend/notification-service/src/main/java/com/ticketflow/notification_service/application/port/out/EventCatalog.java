package com.ticketflow.notification_service.application.port.out;

import com.ticketflow.notification_service.domain.EventDetails;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port de sortie : obtenir les infos d'un événement, sans dire COMMENT (HTTP, cache, faux en test…). */
public interface EventCatalog {

    Mono<EventDetails> findEvent(UUID eventId);
}

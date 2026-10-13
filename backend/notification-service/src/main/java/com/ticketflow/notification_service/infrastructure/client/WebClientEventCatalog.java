package com.ticketflow.notification_service.infrastructure.client;

import com.ticketflow.notification_service.application.port.out.EventCatalog;
import com.ticketflow.notification_service.domain.EventDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Adaptateur du port EventCatalog : appelle event-service en HTTP, sans bloquer de thread. */
@Component
public class WebClientEventCatalog implements EventCatalog {

    private final WebClient webClient;

    public WebClientEventCatalog(WebClient.Builder builder, @Value("${app.event-service.url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<EventDetails> findEvent(UUID eventId) {
        return webClient.get()
                .uri("/events/{id}", eventId)
                .retrieve()
                .bodyToMono(EventDetails.class)
                // 404 = "cet événement n'existe pas" : une réponse normale, pas une panne → Mono vide
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> Mono.empty());
    }
}

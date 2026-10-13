package com.ticketflow.notification_service.infrastructure.client;

import com.ticketflow.notification_service.domain.EventDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientEventCatalogTest {

    private final UUID eventId = UUID.randomUUID();
    private final AtomicReference<ClientRequest> sentRequest = new AtomicReference<>();   // la requête réellement envoyée

    // Un WebClient dont la partie "réseau" est remplacée : il répond ce qu'on veut, sans event-service
    private WebClientEventCatalog catalogAnswering(HttpStatus status, String json) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            sentRequest.set(request);
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .build());
        });
        return new WebClientEventCatalog(builder, "http://event-service:8080");
    }

    @Test
    void lit_les_details_de_l_evenement_et_ignore_les_champs_inutiles() {
        // event-service renvoie plus de champs (location, priceCents…) : ils sont ignorés (tolerant reader)
        WebClientEventCatalog catalog = catalogAnswering(HttpStatus.OK, """
                {"id":"%s","title":"Concert de jazz","location":"Tunis","availableSeats":41,"priceCents":5000}
                """.formatted(eventId));

        StepVerifier.create(catalog.findEvent(eventId))
                .expectNext(new EventDetails(eventId, "Concert de jazz", 41))
                .verifyComplete();
        assertThat(sentRequest.get().url().toString()).isEqualTo("http://event-service:8080/events/" + eventId);
    }

    @Test
    void evenement_introuvable_donne_un_mono_vide_et_pas_une_erreur() {
        StepVerifier.create(catalogAnswering(HttpStatus.NOT_FOUND, "{}").findEvent(eventId))
                .verifyComplete();   // se termine SANS valeur et SANS erreur
    }

    @Test
    void une_panne_d_event_service_reste_une_erreur() {
        // C'est NotificationService qui la rattrapera (onErrorResume → notification simple)
        StepVerifier.create(catalogAnswering(HttpStatus.SERVICE_UNAVAILABLE, "{}").findEvent(eventId))
                .expectError(WebClientResponseException.ServiceUnavailable.class)
                .verify();
    }
}

package com.ticketflow.notification_service.infrastructure.stream;

import com.ticketflow.notification_service.domain.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationStreamControllerTest {

    private final NotificationBroadcaster broadcaster = new NotificationBroadcaster();
    private final NotificationStreamController controller = new NotificationStreamController(broadcaster);

    private final Notification notification =
            new Notification(UUID.randomUUID(), "Concert de jazz", 2, 41, "2 place(s) réservée(s) pour « Concert de jazz » — il en reste 41");

    @Test
    void envoie_chaque_notification_comme_un_evenement_sse_reservation() {
        StepVerifier.create(controller.stream())
                .then(() -> broadcaster.publish(notification))
                .assertNext(event -> {
                    assertThat(event.event()).isEqualTo("reservation");
                    assertThat(event.data()).isEqualTo(notification);
                })
                .thenCancel()   // le flux est infini : on se désabonne, comme un navigateur qui ferme l'onglet
                .verify();
    }

    @Test
    void envoie_un_battement_de_coeur_toutes_les_15_secondes() {
        // Temps virtuel : on "attend" 15 s instantanément
        StepVerifier.withVirtualTime(controller::stream)
                .expectSubscription()
                .thenAwait(NotificationStreamController.HEARTBEAT)
                .assertNext(event -> {
                    assertThat(event.comment()).isEqualTo("keep-alive");
                    assertThat(event.data()).isNull();
                })
                .thenCancel()
                .verify();
    }

    @Test
    void repond_en_text_event_stream() {
        WebTestClient client = WebTestClient.bindToController(controller).build();
        // WebFlux n'envoie les en-têtes HTTP qu'avec le PREMIER événement : on publie peu après la connexion
        Mono.delay(Duration.ofMillis(300)).subscribe(tick -> broadcaster.publish(notification));

        var result = client.get().uri("/notifications/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<Notification>>() { });

        StepVerifier.create(result.getResponseBody())
                .assertNext(event -> assertThat(event.data().eventTitle()).isEqualTo("Concert de jazz"))
                .thenCancel()
                .verify();
    }
}

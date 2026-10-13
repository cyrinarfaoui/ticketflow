package com.ticketflow.notification_service.infrastructure.stream;

import com.ticketflow.notification_service.domain.Notification;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * GET /notifications/stream : la connexion reste OUVERTE, le serveur envoie chaque notification dès qu'elle arrive
 * (Server-Sent Events). Avec WebFlux, une connexion ouverte n'occupe AUCUN thread pendant qu'elle attend.
 */
@RestController
public class NotificationStreamController {

    // Un commentaire SSE régulier empêche les proxys / navigateurs de couper une connexion "silencieuse"
    static final Duration HEARTBEAT = Duration.ofSeconds(15);

    private final NotificationBroadcaster broadcaster;

    public NotificationStreamController(NotificationBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(path = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Notification>> stream() {
        Flux<ServerSentEvent<Notification>> notifications = broadcaster.stream()
                .map(notification -> ServerSentEvent.builder(notification).event("reservation").build());

        Flux<ServerSentEvent<Notification>> heartbeat = Flux.interval(HEARTBEAT)
                .map(tick -> ServerSentEvent.<Notification>builder().comment("keep-alive").build());

        // merge : les deux flux en un seul, dans l'ordre où les valeurs arrivent
        return Flux.merge(notifications, heartbeat);
    }
}

package com.ticketflow.notification_service.application;

import com.ticketflow.notification_service.application.port.in.HandleReservationCreatedUseCase;
import com.ticketflow.notification_service.application.port.out.EventCatalog;
import com.ticketflow.notification_service.application.port.out.NotificationPublisher;
import com.ticketflow.notification_service.domain.Notification;
import com.ticketflow.notification_service.domain.ReservationCreated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class NotificationService implements HandleReservationCreatedUseCase {

    // Au-delà, on n'attend plus event-service : mieux vaut une notification simple que pas de notification
    static final Duration EVENT_LOOKUP_TIMEOUT = Duration.ofSeconds(2);

    private final EventCatalog eventCatalog;
    private final NotificationPublisher publisher;

    public NotificationService(EventCatalog eventCatalog, NotificationPublisher publisher) {
        this.eventCatalog = eventCatalog;
        this.publisher = publisher;
    }

    @Override
    public Mono<Notification> handle(ReservationCreated reservation) {
        return eventCatalog.findEvent(reservation.eventId())
                .timeout(EVENT_LOOKUP_TIMEOUT)                                           // trop lent → erreur
                .map(event -> Notification.enriched(reservation, event))                 // réponse → notification complète
                .switchIfEmpty(Mono.fromSupplier(() -> Notification.basic(reservation))) // introuvable → plan B
                .onErrorResume(error -> {                                                // panne ou timeout → plan B
                    log.warn("Détails de l'événement {} indisponibles ({}) : notification simple",
                            reservation.eventId(), error.toString());
                    return Mono.just(Notification.basic(reservation));
                })
                .doOnNext(notification -> log.info("Notification : {}", notification.message()))
                .doOnNext(publisher::publish);                                           // diffusée en direct (SSE)
    }
}
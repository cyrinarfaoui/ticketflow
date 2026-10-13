package com.ticketflow.notification_service.infrastructure.stream;

import com.ticketflow.notification_service.application.port.out.NotificationPublisher;
import com.ticketflow.notification_service.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Le "haut-parleur" : chaque notification publiée est envoyée à TOUS les navigateurs connectés à cet instant.
 * Sinks = une porte d'entrée pour pousser des valeurs dans un Flux depuis du code non réactif (ici, le listener Kafka).
 */
@Slf4j
@Component
public class NotificationBroadcaster implements NotificationPublisher {

    // multicast : plusieurs abonnés reçoivent chaque valeur ; pas d'historique pour les nouveaux venus.
    // directBestEffort : un abonné trop lent (pas de demande) PERD la valeur, sans ralentir les autres (backpressure).
    private final Sinks.Many<Notification> sink = Sinks.many().multicast().directBestEffort();

    @Override
    public void publish(Notification notification) {
        Sinks.EmitResult result = sink.tryEmitNext(notification);
        // Personne n'écoute : ce n'est pas une erreur, la notification n'intéresse simplement personne
        if (result.isFailure() && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            log.warn("Notification non diffusée ({}) : {}", result, notification.message());
        }
    }

    /** Le flux à écouter. Chaque abonné ne reçoit que les notifications publiées APRÈS son abonnement. */
    public Flux<Notification> stream() {
        return sink.asFlux();
    }
}

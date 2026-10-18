package com.ticketflow.event_service.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Relais de l'Outbox : toutes les secondes, envoie à Kafka les messages pas encore publiés.
 * Livraison "au moins une fois" : un crash entre l'envoi et le marquage renvoie le message au tour suivant ;
 * les consommateurs sont idempotents (ticket #2), un doublon ne débite donc jamais deux fois.
 * Prévu pour UNE instance d'event-service (avec plusieurs instances : SELECT … FOR UPDATE SKIP LOCKED).
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public OutboxRelay(OutboxJpaRepository outbox, KafkaTemplate<String, Object> kafkaTemplate,
                       JsonMapper jsonMapper, Clock clock) {
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        for (OutboxEventEntity message : outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                // Le relais ne connaît pas le type du message : il renvoie le JSON stocké tel quel (arbre JSON)
                JsonNode event = jsonMapper.readTree(message.getPayload());
                // .get() : on ATTEND l'accusé de réception de Kafka avant de marquer le message envoyé
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), event)
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                message.markPublished(clock.instant());
                outbox.save(message);
            } catch (ExecutionException | TimeoutException e) {
                // Kafka indisponible : on s'arrête là (les suivants attendent, pour garder l'ordre) et on réessaie dans 1 s
                log.warn("Envoi impossible du message {} (outbox) : nouvel essai au prochain passage", message.getId(), e);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // arrêt de l'application : on rend la main proprement
                return;
            }
        }
    }
}

package com.ticketflow.event_service.infrastructure.outbox;

import com.ticketflow.event_service.infrastructure.messaging.ReservationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:05Z");

    @Mock private OutboxJpaRepository outbox;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outbox, kafkaTemplate, jsonMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void envoie_les_messages_en_attente_puis_les_marque_envoyes() {
        OutboxEventEntity message = pending("r1");
        when(outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(message));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        relay.publishPending();

        // Le JSON stocké est renvoyé tel quel (arbre JSON) : même contenu que le message d'origine
        verify(kafkaTemplate).send(eq("reservation.created"), eq("r1"), eq(jsonMapper.readTree(jsonMapper.writeValueAsString(event("r1")))));
        assertThat(message.getPublishedAt()).isEqualTo(NOW);
        verify(outbox).save(message);
    }

    @Test
    void kafka_en_panne_le_message_reste_dans_l_outbox_et_l_ordre_est_garde() {
        OutboxEventEntity first = pending("r1");
        OutboxEventEntity second = pending("r2");
        when(outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker injoignable")));

        relay.publishPending();

        assertThat(first.getPublishedAt()).isNull();     // réessayé au prochain passage
        verify(outbox, never()).save(any());
        // On n'envoie pas le 2e avant le 1er : sinon les messages arriveraient dans le désordre
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
        assertThat(second.getPublishedAt()).isNull();
    }

    private OutboxEventEntity pending(String reservationId) {
        return new OutboxEventEntity("reservation.created", reservationId,
                jsonMapper.writeValueAsString(event(reservationId)), Instant.parse("2026-10-18T10:00:00Z"));
    }

    private static ReservationCreatedEvent event(String reservationId) {
        return new ReservationCreatedEvent(2, reservationId, "e1", 2, "PENDING", 10_000L, "TND", "pm_card_visa",
                Instant.parse("2026-10-18T10:00:00Z"));
    }
}

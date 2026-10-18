package com.ticketflow.event_service.infrastructure.outbox;

import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.infrastructure.messaging.RefundRequestedEvent;
import com.ticketflow.event_service.infrastructure.messaging.ReservationCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxReservationEventPublisherTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    @Mock
    private OutboxJpaRepository outbox;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void ecrit_le_message_dans_l_outbox_au_lieu_de_l_envoyer_a_kafka() {
        var publisher = new OutboxReservationEventPublisher(outbox, jsonMapper, Clock.fixed(NOW, ZoneOffset.UTC));
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2);

        publisher.publishCreated(reservation, 10_000L, "pm_card_visa");

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outbox).save(captor.capture());
        OutboxEventEntity message = captor.getValue();
        assertThat(message.getTopic()).isEqualTo("reservation.created");
        assertThat(message.getMessageKey()).isEqualTo(reservation.getId().toString());
        assertThat(message.getCreatedAt()).isEqualTo(NOW);
        assertThat(message.getPublishedAt()).isNull();   // pas encore envoyé : c'est le relais qui s'en charge

        // Le JSON stocké redonne exactement le message attendu par payment-service
        ReservationCreatedEvent event = jsonMapper.readValue(message.getPayload(), ReservationCreatedEvent.class);
        assertThat(event.version()).isEqualTo(2);
        assertThat(event.reservationId()).isEqualTo(reservation.getId().toString());
        assertThat(event.amountCents()).isEqualTo(10_000L);
        assertThat(event.currency()).isEqualTo("TND");
        assertThat(event.paymentMethod()).isEqualTo("pm_card_visa");
    }

    @Test
    void ecrit_la_demande_de_remboursement_dans_l_outbox() {
        var publisher = new OutboxReservationEventPublisher(outbox, jsonMapper, Clock.fixed(NOW, ZoneOffset.UTC));
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2);
        reservation.expire();

        publisher.publishRefundRequested(reservation);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outbox).save(captor.capture());
        OutboxEventEntity message = captor.getValue();
        assertThat(message.getTopic()).isEqualTo("refund.requested");
        assertThat(message.getMessageKey()).isEqualTo(reservation.getId().toString());
        RefundRequestedEvent event = jsonMapper.readValue(message.getPayload(), RefundRequestedEvent.class);
        assertThat(event.reservationId()).isEqualTo(reservation.getId().toString());
        assertThat(event.reason()).isEqualTo("EXPIRED");
    }
}

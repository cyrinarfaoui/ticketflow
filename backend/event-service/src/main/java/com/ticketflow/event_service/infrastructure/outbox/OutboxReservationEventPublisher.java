package com.ticketflow.event_service.infrastructure.outbox;

import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.infrastructure.messaging.RefundRequestedEvent;
import com.ticketflow.event_service.infrastructure.messaging.ReservationCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/**
 * Adaptateur du port ReservationEventPublisher (pattern Outbox).
 * N'envoie RIEN à Kafka : écrit le message dans la table outbox_events, dans la MÊME transaction
 * que le changement de la réservation. Les deux sont donc enregistrés ensemble, ou pas du tout.
 * L'envoi à Kafka est fait ensuite par OutboxRelay.
 * MANDATORY : refuse d'être appelé hors transaction (le message ne serait plus atomique avec la réservation).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxReservationEventPublisher implements ReservationEventPublisher {

    static final String TOPIC_CREATED = "reservation.created";
    static final String TOPIC_REFUND_REQUESTED = "refund.requested";

    private final OutboxJpaRepository outbox;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public OutboxReservationEventPublisher(OutboxJpaRepository outbox, JsonMapper jsonMapper, Clock clock) {
        this.outbox = outbox;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public void publishCreated(Reservation reservation, long amountCents, String paymentMethod) {
        ReservationCreatedEvent event = ReservationCreatedEvent.from(reservation, amountCents, paymentMethod);
        write(TOPIC_CREATED, event.reservationId(), event);
    }

    @Override
    public void publishRefundRequested(Reservation reservation) {
        RefundRequestedEvent event = RefundRequestedEvent.from(reservation, clock.instant());
        write(TOPIC_REFUND_REQUESTED, event.reservationId(), event);
    }

    // Clé = id de la réservation : tous ses messages vont dans la même partition Kafka, donc restent dans l'ordre
    private void write(String topic, String key, Object event) {
        outbox.save(new OutboxEventEntity(topic, key, jsonMapper.writeValueAsString(event), clock.instant()));
    }
}

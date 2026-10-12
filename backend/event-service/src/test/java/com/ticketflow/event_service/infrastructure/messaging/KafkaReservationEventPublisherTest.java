package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.domain.Reservation;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaReservationEventPublisherTest {

    private static final String TOPIC = "reservation.created";

    @Mock
    private KafkaTemplate<String, ReservationCreatedEvent> kafkaTemplate;

    @InjectMocks
    private KafkaReservationEventPublisher publisher;

    private final Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2);

    @Test
    void publie_l_evenement_sur_le_topic_reservation_created() {
        // Arrange : Kafka répond "message bien reçu"
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 42L, 0, 0L, 0, 0);
        SendResult<String, ReservationCreatedEvent> result =
                new SendResult<>(new ProducerRecord<>(TOPIC, "cle", null), metadata);
        when(kafkaTemplate.send(anyString(), anyString(), any(ReservationCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(result));

        // Act
        publisher.publishCreated(reservation, 10_000L, "pm_card_visa");

        // Assert : on récupère le message envoyé pour l'inspecter
        ArgumentCaptor<ReservationCreatedEvent> captor = ArgumentCaptor.forClass(ReservationCreatedEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(reservation.getId().toString()), captor.capture());

        ReservationCreatedEvent event = captor.getValue();
        assertThat(event.version()).isEqualTo(2);
        assertThat(event.amountCents()).isEqualTo(10_000L);
        assertThat(event.currency()).isEqualTo("TND");
        assertThat(event.paymentMethod()).isEqualTo("pm_card_visa");
        assertThat(event.reservationId()).isEqualTo(reservation.getId().toString());
        assertThat(event.eventId()).isEqualTo(reservation.getEventId().toString());
        assertThat(event.seats()).isEqualTo(2);
        assertThat(event.status()).isEqualTo("PENDING");
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void ne_plante_pas_si_kafka_est_indisponible() {
        // Arrange : l'envoi échoue
        when(kafkaTemplate.send(anyString(), anyString(), any(ReservationCreatedEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka indisponible")));

        // Act : ne doit lever aucune exception (l'erreur est seulement journalisée)
        publisher.publishCreated(reservation, 10_000L, "pm_card_visa");

        // Assert
        verify(kafkaTemplate).send(eq(TOPIC), eq(reservation.getId().toString()), any(ReservationCreatedEvent.class));
    }
}
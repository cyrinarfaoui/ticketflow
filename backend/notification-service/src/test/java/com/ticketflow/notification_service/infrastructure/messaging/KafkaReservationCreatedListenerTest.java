package com.ticketflow.notification_service.infrastructure.messaging;

import com.ticketflow.notification_service.application.port.in.HandleReservationCreatedUseCase;
import com.ticketflow.notification_service.domain.ReservationCreated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaReservationCreatedListenerTest {

    @Mock
    private HandleReservationCreatedUseCase useCase;

    @InjectMocks
    private KafkaReservationCreatedListener listener;

    @Test
    void transforme_le_message_kafka_en_evenement_metier_et_le_transmet() {
        // Le faux use case renvoie un Mono vide (sinon null.block() → NullPointerException)
        when(useCase.handle(any())).thenReturn(Mono.empty());
        UUID reservationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        ReservationCreatedMessage message = new ReservationCreatedMessage(
                1, reservationId.toString(), eventId.toString(), 3, "PENDING", Instant.now());

        listener.onMessage(message);

        ArgumentCaptor<ReservationCreated> captor = ArgumentCaptor.forClass(ReservationCreated.class);
        verify(useCase).handle(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new ReservationCreated(reservationId, eventId, 3, "PENDING"));
    }
}
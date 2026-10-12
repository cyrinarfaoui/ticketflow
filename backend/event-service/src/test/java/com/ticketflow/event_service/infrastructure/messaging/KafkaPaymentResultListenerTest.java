package com.ticketflow.event_service.infrastructure.messaging;

import com.ticketflow.event_service.application.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentResultListenerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private KafkaPaymentResultListener listener;

    private final UUID reservationId = UUID.randomUUID();

    @Test
    void paiement_reussi_confirme_la_reservation() {
        listener.onPaymentResult(message("SUCCEEDED", null));

        verify(reservationService).confirmPayment(reservationId);
        verify(reservationService, never()).rejectPayment(any(), any());
    }

    @Test
    void paiement_refuse_declenche_la_compensation_avec_le_motif() {
        listener.onPaymentResult(message("DECLINED", "expired_card"));

        verify(reservationService).rejectPayment(reservationId, "expired_card");
        verify(reservationService, never()).confirmPayment(any());
    }

    private PaymentResultMessage message(String status, String declineCode) {
        return new PaymentResultMessage(1, UUID.randomUUID().toString(), reservationId.toString(),
                status, declineCode, 10_000L, "TND", Instant.now());
    }
}

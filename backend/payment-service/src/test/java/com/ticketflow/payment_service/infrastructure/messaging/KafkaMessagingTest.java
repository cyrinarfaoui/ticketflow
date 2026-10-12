package com.ticketflow.payment_service.infrastructure.messaging;

import com.ticketflow.payment_service.application.PaymentService;
import com.ticketflow.payment_service.domain.DeclineCode;
import com.ticketflow.payment_service.domain.Money;
import com.ticketflow.payment_service.domain.Payment;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaMessagingTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private PaymentService paymentService;

    @Mock
    private KafkaTemplate<String, PaymentResultMessage> kafkaTemplate;

    private final UUID reservationId = UUID.randomUUID();

    private ReservationCreatedMessage message(Long amountCents, String currency, String paymentMethod) {
        return new ReservationCreatedMessage(2, reservationId.toString(), UUID.randomUUID().toString(), 2,
                "PENDING", amountCents, currency, paymentMethod, NOW);
    }

    // ---------- Entrée : reservation.created ----------

    @Test
    void transforme_le_message_en_demande_de_paiement() {
        new KafkaReservationCreatedListener(paymentService).onReservationCreated(message(9_000L, "TND", "pm_card_visa"));

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).process(captor.capture());
        assertThat(captor.getValue().reservationId()).isEqualTo(reservationId);
        assertThat(captor.getValue().amount()).isEqualTo(new Money(9_000, "TND"));
        assertThat(captor.getValue().paymentMethod()).isEqualTo("pm_card_visa");
    }

    @Test
    void ignore_un_ancien_message_sans_montant() {
        new KafkaReservationCreatedListener(paymentService).onReservationCreated(message(null, null, null));

        verifyNoInteractions(paymentService);
    }

    // ---------- Sortie : payment.completed / payment.failed ----------

    private Payment payment(PaymentResult result) {
        return Payment.of(new PaymentRequest(reservationId, new Money(9_000, "TND"), "pm_card_visa"), result, NOW);
    }

    @Test
    void un_paiement_accepte_part_sur_payment_completed() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentResultMessage.class)))
                .thenReturn(new CompletableFuture<>());

        new KafkaPaymentEventPublisher(kafkaTemplate, CLOCK).publish(payment(PaymentResult.succeeded()));

        ArgumentCaptor<PaymentResultMessage> captor = ArgumentCaptor.forClass(PaymentResultMessage.class);
        verify(kafkaTemplate).send(eq("payment.completed"), eq(reservationId.toString()), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("SUCCEEDED");
        assertThat(captor.getValue().declineCode()).isNull();
    }

    @Test
    void un_refus_part_sur_payment_failed_avec_le_code_stripe() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentResultMessage.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka indisponible")));

        // Ne plante pas si Kafka est indisponible : l'erreur est journalisée
        new KafkaPaymentEventPublisher(kafkaTemplate, CLOCK).publish(payment(PaymentResult.declined(DeclineCode.EXPIRED_CARD)));

        ArgumentCaptor<PaymentResultMessage> captor = ArgumentCaptor.forClass(PaymentResultMessage.class);
        verify(kafkaTemplate).send(eq("payment.failed"), eq(reservationId.toString()), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("DECLINED");
        assertThat(captor.getValue().declineCode()).isEqualTo("expired_card");
    }
}

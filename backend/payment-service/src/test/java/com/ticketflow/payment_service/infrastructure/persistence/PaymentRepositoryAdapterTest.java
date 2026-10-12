package com.ticketflow.payment_service.infrastructure.persistence;

import com.ticketflow.payment_service.domain.DeclineCode;
import com.ticketflow.payment_service.domain.Money;
import com.ticketflow.payment_service.domain.Payment;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contre une vraie base Postgres (paymentdb) ; chaque test est annulé à la fin (@Transactional). */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Transactional
class PaymentRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Autowired
    private PaymentRepositoryAdapter adapter;

    @Autowired
    private PaymentJpaRepository jpaRepository;

    private Payment payment(UUID reservationId, PaymentResult result) {
        return Payment.of(new PaymentRequest(reservationId, new Money(4_500, "TND"), "pm_card_visa"), result, NOW);
    }

    @Test
    void enregistre_et_retrouve_un_paiement_par_sa_reservation() {
        UUID reservationId = UUID.randomUUID();
        adapter.save(payment(reservationId, PaymentResult.declined(DeclineCode.INSUFFICIENT_FUNDS)));

        Payment found = adapter.findByReservationId(reservationId).orElseThrow();

        assertThat(found.amount()).isEqualTo(new Money(4_500, "TND"));
        assertThat(found.result().declineCode()).isEqualTo(DeclineCode.INSUFFICIENT_FUNDS);
        assertThat(found.createdAt().truncatedTo(ChronoUnit.SECONDS)).isEqualTo(NOW);
    }

    @Test
    void rien_pour_une_reservation_jamais_payee() {
        assertThat(adapter.findByReservationId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void la_base_refuse_un_deuxieme_paiement_pour_la_meme_reservation() {
        UUID reservationId = UUID.randomUUID();
        adapter.save(payment(reservationId, PaymentResult.succeeded()));

        // Même si le code applicatif se trompait (deux messages au même instant), la contrainte UNIQUE protège
        assertThatThrownBy(() -> {
            adapter.save(payment(reservationId, PaymentResult.succeeded()));
            jpaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}

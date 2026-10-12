package com.ticketflow.event_service.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void should_start_in_pending_state() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 3);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void should_move_to_confirmed_when_confirming_a_pending_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 3);

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void should_move_to_cancelled_when_cancelling_a_pending_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 3);

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void should_move_to_cancelled_when_cancelling_a_confirmed_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 3);
        reservation.confirm();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void should_throw_exception_when_confirming_an_already_cancelled_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 3);
        reservation.cancel();

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("annulée");
    }

    @Test
    void should_throw_exception_when_cancelling_an_already_cancelled_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 3);
        reservation.cancel();

        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà annulée");
    }

    @Test
    void une_nouvelle_reservation_appartient_a_l_utilisateur_qui_la_cree() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2);

        assertThat(reservation.getUserId()).isEqualTo("user-1");
        assertThat(reservation.isOwnedBy("user-1")).isTrue();
        assertThat(reservation.isOwnedBy("user-2")).isFalse();
    }

    @Test
    void une_nouvelle_reservation_sans_utilisateur_est_refusee() {
        assertThatThrownBy(() -> new Reservation(UUID.randomUUID(), UUID.randomUUID(), " ", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("utilisateur");
    }

    // ---------- Saga de paiement (ticket #2) ----------

    @Test
    void echec_de_paiement_annule_la_reservation_avec_le_motif() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2);

        reservation.failPayment("insufficient_funds");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getPaymentFailureCode()).isEqualTo("insufficient_funds");
        assertThat(reservation.isPending()).isFalse();
    }

    @Test
    void une_reservation_confirmee_ne_peut_plus_echouer_au_paiement() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2);
        reservation.confirm();

        assertThatThrownBy(() -> reservation.failPayment("card_declined"))
                .isInstanceOf(IllegalStateException.class);
    }
}

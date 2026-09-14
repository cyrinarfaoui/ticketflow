package com.ticketflow.event_service.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void should_start_in_pending_state() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), 3);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void should_move_to_confirmed_when_confirming_a_pending_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), 3);

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void should_move_to_cancelled_when_cancelling_a_pending_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), 3);

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void should_move_to_cancelled_when_cancelling_a_confirmed_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), 3);
        reservation.confirm();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void should_throw_exception_when_confirming_an_already_cancelled_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), 3);
        reservation.cancel();

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("annulée");
    }

    @Test
    void should_throw_exception_when_cancelling_an_already_cancelled_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), 3);
        reservation.cancel();

        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà annulée");
    }
}
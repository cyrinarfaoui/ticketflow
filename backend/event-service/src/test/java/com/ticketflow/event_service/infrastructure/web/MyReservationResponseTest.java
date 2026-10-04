package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.application.ReservationWithEvent;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MyReservationResponseTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2030, 1, 1, 20, 0);

    @Test
    void reprend_la_reservation_et_les_infos_de_l_evenement() {
        Event event = new Event(UUID.randomUUID(), "Concert", "", "Tunis", EventCategory.CONCERT, DATE, 100);
        Reservation reservation = new Reservation(UUID.randomUUID(), event.getId(), "user-1", 2);

        MyReservationResponse response = MyReservationResponse.from(new ReservationWithEvent(reservation, event));

        assertThat(response.id()).isEqualTo(reservation.getId());
        assertThat(response.eventTitle()).isEqualTo("Concert");
        assertThat(response.eventLocation()).isEqualTo("Tunis");
        assertThat(response.eventDate()).isEqualTo(DATE);
        assertThat(response.seats()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.createdAt()).isEqualTo(reservation.getCreatedAt());
    }

    @Test
    void reste_affichable_si_l_evenement_n_existe_plus() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 1);

        MyReservationResponse response = MyReservationResponse.from(new ReservationWithEvent(reservation, null));

        assertThat(response.eventTitle()).isEqualTo("Événement supprimé");
        assertThat(response.eventLocation()).isNull();
        assertThat(response.eventDate()).isNull();
    }
}

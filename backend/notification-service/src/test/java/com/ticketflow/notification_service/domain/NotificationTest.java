package com.ticketflow.notification_service.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    private static Notification enriched(int seats, int availableSeats) {
        var reservation = new ReservationCreated(UUID.randomUUID(), EVENT_ID, seats, "PENDING");
        return Notification.enriched(reservation, new EventDetails(EVENT_ID, "Festival", availableSeats));
    }

    @Test
    void accorde_au_singulier_pour_une_seule_place() {
        assertThat(enriched(1, 1).message())
                .isEqualTo("« Festival » : 1 place vient d'être réservée. Plus qu'une place disponible !");
    }

    @Test
    void annonce_que_l_evenement_est_complet() {
        assertThat(enriched(4, 0).message())
                .isEqualTo("« Festival » : 4 places viennent d'être réservées. L'événement est complet !");
    }

    @Test
    void notification_simple_au_singulier() {
        var reservation = new ReservationCreated(UUID.randomUUID(), EVENT_ID, 1, "PENDING");

        assertThat(Notification.basic(reservation).message()).isEqualTo("1 place vient d'être réservée.");
    }
}

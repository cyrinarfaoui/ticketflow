package com.ticketflow.event_service.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReservationStatsTest {

    @Test
    void calcule_le_total_et_le_taux_d_expiration() {
        ReservationStats stats = new ReservationStats(3, Map.of(
                ReservationStatus.CONFIRMED, 6L,
                ReservationStatus.EXPIRED, 2L,
                ReservationStatus.PENDING, 2L
        ));

        assertThat(stats.total()).isEqualTo(10);
        assertThat(stats.expirationRate()).isCloseTo(0.2, within(0.0001));
    }

    @Test
    void un_statut_absent_compte_zero() {
        ReservationStats stats = new ReservationStats(0, Map.of(ReservationStatus.CONFIRMED, 4L));

        assertThat(stats.count(ReservationStatus.CANCELLED)).isZero();
    }

    @Test
    void taux_nul_quand_il_n_y_a_aucune_reservation() {
        ReservationStats stats = new ReservationStats(0, Map.of());

        assertThat(stats.expirationRate()).isZero();
    }
}

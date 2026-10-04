package com.ticketflow.event_service.infrastructure.batch;

import com.ticketflow.event_service.domain.ReservationStatus;
import com.ticketflow.event_service.infrastructure.ReservationEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpireReservationsProcessorTest {

    @Test
    void expire_la_reservation_sans_perdre_son_proprietaire_ni_sa_date() throws Exception {
        Instant creation = Instant.parse("2026-10-04T08:00:00Z");
        ReservationEntity pending = new ReservationEntity(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2,
                ReservationStatus.PENDING, creation);

        ReservationEntity expired = new ExpireReservationsJobConfig().expireReservationProcessor().process(pending);

        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expired.getUserId()).isEqualTo("user-1");   // le bug évité : le propriétaire était perdu
        assertThat(expired.getCreatedAt()).isEqualTo(creation);
        assertThat(expired.getId()).isEqualTo(pending.getId());
    }
}

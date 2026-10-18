package com.ticketflow.event_service.infrastructure.batch;

import com.ticketflow.event_service.application.ReservationService;
import com.ticketflow.event_service.domain.ReservationStatus;
import com.ticketflow.event_service.infrastructure.ReservationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpireReservationsWriterTest {

    @Test
    void chaque_reservation_du_paquet_passe_par_le_service() throws Exception {
        // ARRANGE : un paquet ("chunk") de 2 réservations trouvées par le Reader
        ReservationService service = mock(ReservationService.class);
        ReservationEntity first = pending();
        ReservationEntity second = pending();

        // ACT : le batch écrit le paquet
        new ExpireReservationsJobConfig().expiredReservationsWriter(service).write(new Chunk<>(first, second));

        // ASSERT : le service (qui expire ET rend les places) est appelé pour chacune
        verify(service).expireReservation(first.getId());
        verify(service).expireReservation(second.getId());
    }

    private static ReservationEntity pending() {
        return new ReservationEntity(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2,
                ReservationStatus.PENDING, Instant.parse("2026-10-04T08:00:00Z"));
    }
}

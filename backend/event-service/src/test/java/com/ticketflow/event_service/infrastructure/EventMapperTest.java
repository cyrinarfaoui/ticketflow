package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.EventStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

    @Test
    void relit_un_evenement_cloture_avec_des_places_deja_reservees() {
        // 50 places, 20 déjà réservées, puis clôturé : l'ordre book() puis close() compte dans le mapper
        EventEntity entity = new EventEntity(UUID.randomUUID(), "Concert", "", "Tunis", EventCategory.CONCERT,
                LocalDateTime.of(2030, 1, 1, 20, 0), 50, 30, EventStatus.CLOSED, 5_000L);

        Event event = EventMapper.toDomain(entity);

        assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
        assertThat(event.getAvailableSeats()).isEqualTo(30);
        assertThat(EventMapper.toEntity(event).getStatus()).isEqualTo(EventStatus.CLOSED);
    }
}

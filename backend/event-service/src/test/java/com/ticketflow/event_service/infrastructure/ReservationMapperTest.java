package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationMapperTest {


    private static final UUID ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();

    @Test
    void toEntity_copie_tous_les_champs_du_domaine() {
        Reservation reservation = new Reservation(ID, EVENT_ID, 3);

        ReservationEntity entity = ReservationMapper.toEntity(reservation);

        assertThat(entity.getId()).isEqualTo(ID);
        assertThat(entity.getEventId()).isEqualTo(EVENT_ID);
        assertThat(entity.getSeats()).isEqualTo(3);
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(entity.getCreatedAt()).isNotNull();
    }


    @ParameterizedTest
    @EnumSource(ReservationStatus.class)
    void toDomain_conserve_le_statut_enregistre_en_base(ReservationStatus statut) {
        ReservationEntity entity = new ReservationEntity(ID, EVENT_ID, 2, statut);

        Reservation reservation = ReservationMapper.toDomain(entity);

        assertThat(reservation.getId()).isEqualTo(ID);
        assertThat(reservation.getEventId()).isEqualTo(EVENT_ID);
        assertThat(reservation.getSeats()).isEqualTo(2);
        assertThat(reservation.getStatus()).isEqualTo(statut);
    }


}

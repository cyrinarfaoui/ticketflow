package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.Reservation;

public class ReservationMapper {

    public static ReservationEntity toEntity(Reservation reservation) {
        return new ReservationEntity(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getSeats(),
                reservation.getStatus()
        );
    }

    public static Reservation toDomain(ReservationEntity entity) {
        Reservation reservation = new Reservation(entity.getId(), entity.getEventId(), entity.getSeats());
        switch (entity.getStatus()) {
            case CONFIRMED -> reservation.confirm();
            case CANCELLED -> reservation.cancel();
            case EXPIRED -> reservation.expire();
            case PENDING -> { /* état initial : rien à faire */ }
        }
        return reservation;
    }
}

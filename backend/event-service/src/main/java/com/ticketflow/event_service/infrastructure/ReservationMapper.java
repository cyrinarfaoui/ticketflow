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
        if (entity.getStatus() == com.ticketflow.event_service.domain.ReservationStatus.CONFIRMED) {
            reservation.confirm();
        } else if (entity.getStatus() == com.ticketflow.event_service.domain.ReservationStatus.CANCELLED) {
            reservation.cancel();
        }
        return reservation;
    }
}

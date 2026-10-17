package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.Reservation;

public class ReservationMapper {

    public static ReservationEntity toEntity(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getUserId(),
                reservation.getSeats(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getPaymentFailureCode()
        );
        entity.setUserEmail(reservation.getUserEmail());
        return entity;
    }

    public static Reservation toDomain(ReservationEntity entity) {
        Reservation reservation = new Reservation(
                entity.getId(), entity.getEventId(), entity.getUserId(), entity.getSeats(), entity.getCreatedAt()
        );
        reservation.setContactEmail(entity.getUserEmail());
        switch (entity.getStatus()) {
            case CONFIRMED -> reservation.confirm();
            // Annulée pour refus de paiement : on rejoue failPayment pour retrouver le motif
            case CANCELLED -> {
                if (entity.getPaymentFailureCode() != null) {
                    reservation.failPayment(entity.getPaymentFailureCode());
                } else {
                    reservation.cancel();
                }
            }
            case EXPIRED -> reservation.expire();
            case PENDING -> { /* état initial : rien à faire */ }
        }
        return reservation;
    }
}

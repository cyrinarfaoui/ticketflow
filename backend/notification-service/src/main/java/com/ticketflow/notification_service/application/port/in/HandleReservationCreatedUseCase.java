package com.ticketflow.notification_service.application.port.in;

import com.ticketflow.notification_service.domain.ReservationCreated;

public interface HandleReservationCreatedUseCase {

    void handle(ReservationCreated event);
}

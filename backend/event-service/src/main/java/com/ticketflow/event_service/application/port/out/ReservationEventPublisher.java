package com.ticketflow.event_service.application.port.out;

import com.ticketflow.event_service.domain.Reservation;

public interface ReservationEventPublisher {

    void publishCreated(Reservation reservation);
}

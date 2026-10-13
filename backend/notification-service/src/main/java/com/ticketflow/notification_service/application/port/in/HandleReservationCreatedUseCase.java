package com.ticketflow.notification_service.application.port.in;

import com.ticketflow.notification_service.domain.Notification;
import com.ticketflow.notification_service.domain.ReservationCreated;
import reactor.core.publisher.Mono;

public interface HandleReservationCreatedUseCase {

    Mono<Notification> handle(ReservationCreated event);
}

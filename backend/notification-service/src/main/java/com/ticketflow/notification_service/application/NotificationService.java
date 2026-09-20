package com.ticketflow.notification_service.application;


import com.ticketflow.notification_service.application.port.in.HandleReservationCreatedUseCase;
import com.ticketflow.notification_service.domain.ReservationCreated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService implements HandleReservationCreatedUseCase {

    @Override
    public void handle(ReservationCreated event) {
        log.info("Notification : réservation {} ({} place(s)) pour l'événement {}, statut {}",
                event.reservationId(), event.seats(), event.eventId(), event.status());
    }


}

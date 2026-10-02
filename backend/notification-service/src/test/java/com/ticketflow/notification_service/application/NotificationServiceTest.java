package com.ticketflow.notification_service.application;

import com.ticketflow.notification_service.domain.ReservationCreated;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationServiceTest {

    @Test
    void traite_une_reservation_creee_sans_erreur() {
        NotificationService service = new NotificationService();
        ReservationCreated event = new ReservationCreated(UUID.randomUUID(), UUID.randomUUID(), 1, "PENDING");

        assertThatCode(() -> service.handle(event)).doesNotThrowAnyException();
    }
}
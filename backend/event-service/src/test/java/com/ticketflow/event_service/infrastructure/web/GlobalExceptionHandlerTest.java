package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.application.ReservationAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void transforme_un_acces_refuse_en_403_avec_le_message() {
        var response = handler.handleAccessDenied(new ReservationAccessDeniedException("Pas à vous"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "Pas à vous");
    }
}

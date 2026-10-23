package com.ticketflow.event_service.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void une_erreur_de_validation_renvoie_le_message_du_champ_et_pas_bad_request() throws Exception {
        var errors = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "createEventRequest");
        errors.addError(new org.springframework.validation.FieldError("createEventRequest", "date",
                "La date de l'événement doit être dans le futur"));
        var parameter = new org.springframework.core.MethodParameter(
                EventController.class.getMethod("updateEvent", java.util.UUID.class, CreateEventRequest.class), 1);

        var response = handler.handleValidation(
                new org.springframework.web.bind.MethodArgumentNotValidException(parameter, errors));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "La date de l'événement doit être dans le futur");
    }
}

package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.domain.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateEventRequest(

        @NotBlank String title,
        String description,
        @NotBlank String location,
        @NotNull EventCategory category,
        @NotNull @Future LocalDateTime date,
        @Positive int totalSeats

) {
}

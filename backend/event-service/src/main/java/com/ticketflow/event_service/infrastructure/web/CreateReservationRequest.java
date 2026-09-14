package com.ticketflow.event_service.infrastructure.web;

import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(@Positive int seats) {
}

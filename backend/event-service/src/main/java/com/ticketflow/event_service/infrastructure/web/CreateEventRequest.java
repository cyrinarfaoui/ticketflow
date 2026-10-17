package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.domain.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateEventRequest(

        @NotBlank(message = "Le titre est obligatoire") String title,
        String description,
        @NotBlank(message = "Le lieu est obligatoire") String location,
        @NotNull(message = "La catégorie est obligatoire") EventCategory category,
        @NotNull(message = "La date est obligatoire")
        @Future(message = "La date de l'événement doit être dans le futur") LocalDateTime date,
        @Positive(message = "Il faut au moins 1 place") int totalSeats,
        // Prix d'une place en centimes (4500 = 45,00 TND)
        @Positive(message = "Le prix doit être supérieur à 0") long priceCents

) {
}

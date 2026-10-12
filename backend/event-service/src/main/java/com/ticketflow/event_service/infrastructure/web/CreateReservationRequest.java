package com.ticketflow.event_service.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * paymentMethod : un JETON de moyen de paiement (pm_…) fourni par le prestataire, jamais un numéro de carte.
 * Le @Pattern refuse tout ce qui ressemble à autre chose qu'un jeton (par exemple des chiffres de carte).
 */
public record CreateReservationRequest(
        @Positive int seats,
        @NotBlank @Pattern(regexp = "pm_[A-Za-z0-9_]{1,60}", message = "Moyen de paiement invalide") String paymentMethod
) {
}

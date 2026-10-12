package com.ticketflow.payment_service.domain;

import java.util.UUID;

/**
 * Une demande de paiement. paymentMethod est un JETON (pm_…) fourni par le prestataire :
 * jamais un numéro de carte (norme PCI-DSS).
 */
public record PaymentRequest(UUID reservationId, Money amount, String paymentMethod) {
}
package com.ticketflow.event_service.domain;

import java.time.Duration;

/**
 * Bilan d'une annulation d'événement.
 * notified + failed + withoutEmail = cancelledReservations : aucun client n'est oublié dans le compte.
 */
public record CancellationReport(int cancelledReservations, int notified, int failed, int withoutEmail, Duration duration) {
}

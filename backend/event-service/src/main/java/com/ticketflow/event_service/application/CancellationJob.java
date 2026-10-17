package com.ticketflow.event_service.application;

import com.ticketflow.event_service.domain.CancellationReport;

/** État d'une annulation lancée en arrière-plan : en cours, terminée (avec le bilan) ou échouée (avec l'erreur). */
public record CancellationJob(Status status, CancellationReport report, String error) {

    public enum Status { RUNNING, DONE, FAILED }

    static CancellationJob running() {
        return new CancellationJob(Status.RUNNING, null, null);
    }

    static CancellationJob done(CancellationReport report) {
        return new CancellationJob(Status.DONE, report, null);
    }

    static CancellationJob failed(String error) {
        return new CancellationJob(Status.FAILED, null, error);
    }
}
package com.ticketflow.event_service.application;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lance les annulations en arrière-plan (thread virtuel) et garde leur état pour le suivi.
 * Limite assumée : l'état est en mémoire, donc perdu au redémarrage et propre à UNE instance du service
 * (avec plusieurs instances, on le stockerait en base ou dans Redis).
 */
@Service
public class EventCancellationJobs {

    private final EventService eventService;
    private final EventCancellationService cancellationService;
    // Thread-safe : lue par les requêtes HTTP et écrite par le thread d'arrière-plan en même temps
    private final Map<UUID, CancellationJob> jobs = new ConcurrentHashMap<>();

    public EventCancellationJobs(EventService eventService, EventCancellationService cancellationService) {
        this.eventService = eventService;
        this.cancellationService = cancellationService;
    }

    public CancellationJob start(UUID eventId) {
        eventService.getEvent(eventId);   // événement inconnu → exception tout de suite (404), avant de lancer quoi que ce soit

        boolean[] launch = {false};
        // compute est ATOMIQUE : deux clics simultanés sur "Annuler" ne lancent pas deux annulations
        CancellationJob job = jobs.compute(eventId, (id, current) -> {
            if (current != null && current.status() != CancellationJob.Status.FAILED) {
                return current;                       // déjà en cours ou terminée : on renvoie l'état existant
            }
            launch[0] = true;
            return CancellationJob.running();
        });

        if (launch[0]) {
            Thread.ofVirtual().name("cancel-event-" + eventId).start(() -> run(eventId));
        }
        return job;
    }

    public Optional<CancellationJob> find(UUID eventId) {
        return Optional.ofNullable(jobs.get(eventId));
    }

    private void run(UUID eventId) {
        try {
            jobs.put(eventId, CancellationJob.done(cancellationService.cancelEvent(eventId)));
        } catch (RuntimeException e) {
            jobs.put(eventId, CancellationJob.failed(e.getMessage()));   // ex. "Cet événement est déjà annulé"
        }
    }
}
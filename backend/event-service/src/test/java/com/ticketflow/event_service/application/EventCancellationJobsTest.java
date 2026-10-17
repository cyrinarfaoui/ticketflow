package com.ticketflow.event_service.application;

import com.ticketflow.event_service.domain.CancellationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventCancellationJobsTest {

    @Mock private EventService eventService;
    @Mock private EventCancellationService cancellationService;

    private final UUID eventId = UUID.randomUUID();
    private final CancellationReport report = new CancellationReport(3, 3, 0, 0, Duration.ofMillis(60));

    // Attend (2 s max) que l'annulation d'arrière-plan atteigne l'état voulu
    private CancellationJob waitFor(EventCancellationJobs jobs, CancellationJob.Status status) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            CancellationJob job = jobs.find(eventId).orElseThrow();
            if (job.status() == status) {
                return job;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Statut " + status + " jamais atteint");
    }

    @Test
    void repond_tout_de_suite_running_puis_done_avec_le_bilan() throws Exception {
        CountDownLatch release = new CountDownLatch(1);   // "feu rouge" : bloque l'annulation tant qu'on n'a pas vérifié RUNNING
        when(cancellationService.cancelEvent(eventId)).thenAnswer(invocation -> {
            release.await(2, TimeUnit.SECONDS);
            return report;
        });
        EventCancellationJobs jobs = new EventCancellationJobs(eventService, cancellationService);

        CancellationJob started = jobs.start(eventId);

        assertThat(started.status()).isEqualTo(CancellationJob.Status.RUNNING);   // réponse immédiate, sans attendre
        release.countDown();                                                       // feu vert
        assertThat(waitFor(jobs, CancellationJob.Status.DONE).report()).isEqualTo(report);
    }

    @Test
    void un_double_clic_ne_lance_qu_une_seule_annulation() throws Exception {
        when(cancellationService.cancelEvent(eventId)).thenReturn(report);
        EventCancellationJobs jobs = new EventCancellationJobs(eventService, cancellationService);

        jobs.start(eventId);
        jobs.start(eventId);
        waitFor(jobs, CancellationJob.Status.DONE);

        verify(cancellationService, times(1)).cancelEvent(eventId);
    }

    @Test
    void un_echec_est_garde_avec_son_message() throws Exception {
        when(cancellationService.cancelEvent(eventId)).thenThrow(new IllegalStateException("Cet événement est déjà annulé"));
        EventCancellationJobs jobs = new EventCancellationJobs(eventService, cancellationService);

        jobs.start(eventId);

        assertThat(waitFor(jobs, CancellationJob.Status.FAILED).error()).isEqualTo("Cet événement est déjà annulé");
    }
}
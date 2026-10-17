package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.application.CancellationJob;
import com.ticketflow.event_service.application.EventCancellationJobs;
import com.ticketflow.event_service.application.EventService;
import com.ticketflow.event_service.domain.CancellationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock private EventService eventService;
    @Mock private EventCancellationJobs cancellationJobs;

    private EventController controller;
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new EventController(eventService, cancellationJobs);
    }

    @Test
    void annuler_repond_202_avec_l_adresse_de_suivi() {
        CancellationJob running = new CancellationJob(CancellationJob.Status.RUNNING, null, null);
        when(cancellationJobs.start(eventId)).thenReturn(running);

        var response = controller.cancelEvent(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getLocation()).hasToString("/events/" + eventId + "/cancellation");
        assertThat(response.getBody()).isEqualTo(running);
    }

    @Test
    void le_suivi_renvoie_l_etat_et_le_bilan() {
        CancellationReport report = new CancellationReport(3, 2, 1, 0, Duration.ofMillis(80));
        CancellationJob done = new CancellationJob(CancellationJob.Status.DONE, report, null);
        when(cancellationJobs.find(eventId)).thenReturn(Optional.of(done));

        var response = controller.cancellationStatus(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().report().notified()).isEqualTo(2);
    }

    @Test
    void le_suivi_repond_404_si_aucune_annulation_n_a_ete_lancee() {
        when(cancellationJobs.find(eventId)).thenReturn(Optional.empty());

        assertThat(controller.cancellationStatus(eventId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
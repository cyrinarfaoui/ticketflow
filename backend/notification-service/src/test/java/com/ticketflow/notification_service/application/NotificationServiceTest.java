package com.ticketflow.notification_service.application;

import com.ticketflow.notification_service.application.port.out.EventCatalog;
import com.ticketflow.notification_service.domain.EventDetails;
import com.ticketflow.notification_service.domain.Notification;
import com.ticketflow.notification_service.domain.ReservationCreated;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {

    private final UUID eventId = UUID.randomUUID();
    private final ReservationCreated reservation = new ReservationCreated(UUID.randomUUID(), eventId, 2, "PENDING");
    private final List<Notification> published = new ArrayList<>();   // faux NotificationPublisher : garde ce qu'on lui donne

    private NotificationService service(EventCatalog catalog) {
        return new NotificationService(catalog, published::add);
    }

    @Test
    void enrichit_la_notification_avec_le_titre_et_les_places_restantes() {
        // EventCatalog n'a qu'une méthode : une lambda suffit comme faux (pas besoin de Mockito)
        EventCatalog catalog = id -> Mono.just(new EventDetails(id, "Concert de jazz", 41));

        StepVerifier.create(service(catalog).handle(reservation))
                .assertNext(notification -> {
                    assertThat(notification.eventTitle()).isEqualTo("Concert de jazz");
                    assertThat(notification.availableSeats()).isEqualTo(41);
                    assertThat(notification.message()).isEqualTo("2 place(s) réservée(s) pour « Concert de jazz » — il en reste 41");
                })
                .verifyComplete();
    }

    @Test
    void notification_simple_si_event_service_est_en_panne() {
        EventCatalog catalog = id -> Mono.error(new IllegalStateException("503 Service Unavailable"));

        StepVerifier.create(service(catalog).handle(reservation))
                .assertNext(notification -> {
                    assertThat(notification.eventTitle()).isNull();
                    assertThat(notification.message()).isEqualTo("2 place(s) viennent d'être réservée(s)");
                })
                .verifyComplete();   // pas d'erreur : elle a été remplacée par le plan B
    }

    @Test
    void notification_simple_si_l_evenement_est_introuvable() {
        EventCatalog catalog = id -> Mono.empty();

        StepVerifier.create(service(catalog).handle(reservation))
                .assertNext(notification -> assertThat(notification.eventTitle()).isNull())
                .verifyComplete();
    }

    @Test
    void n_attend_pas_plus_de_2_secondes_un_event_service_trop_lent() {
        EventCatalog catalog = id -> Mono.never();   // ne répond JAMAIS

        // Temps VIRTUEL : on avance l'horloge de Reactor de 2 s instantanément, le test ne dure pas 2 s
        StepVerifier.withVirtualTime(() -> service(catalog).handle(reservation))
                .expectSubscription()
                .thenAwait(NotificationService.EVENT_LOOKUP_TIMEOUT)
                .assertNext(notification -> assertThat(notification.eventTitle()).isNull())
                .verifyComplete();
    }

    @Test
    void diffuse_la_notification_une_fois_construite() {
        EventCatalog catalog = id -> Mono.just(new EventDetails(id, "Concert de jazz", 41));

        StepVerifier.create(service(catalog).handle(reservation))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(published).extracting(Notification::eventTitle).containsExactly("Concert de jazz");
    }

    @Test
    void ne_diffuse_rien_tant_que_personne_ne_s_abonne() {
        EventCatalog catalog = id -> Mono.just(new EventDetails(id, "Concert de jazz", 41));

        service(catalog).handle(reservation);   // la recette est créée… mais jamais lancée

        assertThat(published).isEmpty();
    }
}

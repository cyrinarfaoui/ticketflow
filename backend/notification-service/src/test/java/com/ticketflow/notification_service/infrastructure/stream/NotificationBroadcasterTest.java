package com.ticketflow.notification_service.infrastructure.stream;

import com.ticketflow.notification_service.domain.Notification;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationBroadcasterTest {

    private final NotificationBroadcaster broadcaster = new NotificationBroadcaster();

    private static Notification notification(String message) {
        return new Notification(UUID.randomUUID(), "Concert", 1, 10, message);
    }

    @Test
    void chaque_abonne_recoit_la_notification() {
        // Deux navigateurs connectés : StepVerifier s'abonne, puis on publie pendant qu'ils écoutent
        var premier = StepVerifier.create(broadcaster.stream().take(1)).expectNextMatches(n -> n.message().equals("A")).expectComplete();
        var second = StepVerifier.create(broadcaster.stream().take(1)).expectNextMatches(n -> n.message().equals("A")).expectComplete();

        // verifyLater : les abonnements sont actifs, la vérification se fera plus tard
        var premierEnCours = premier.verifyLater();
        var secondEnCours = second.verifyLater();
        broadcaster.publish(notification("A"));

        premierEnCours.verify(Duration.ofSeconds(1));
        secondEnCours.verify(Duration.ofSeconds(1));
    }

    @Test
    void un_nouvel_abonne_ne_recoit_pas_les_anciennes_notifications() {
        broadcaster.publish(notification("ancienne"));   // personne n'écoute encore

        StepVerifier.create(broadcaster.stream().take(1))
                .then(() -> broadcaster.publish(notification("nouvelle")))
                .expectNextMatches(n -> n.message().equals("nouvelle"))
                .verifyComplete();
    }

    @Test
    void publier_sans_aucun_abonne_n_est_pas_une_erreur() {
        assertThatCode(() -> broadcaster.publish(notification("personne n'écoute"))).doesNotThrowAnyException();
    }
}

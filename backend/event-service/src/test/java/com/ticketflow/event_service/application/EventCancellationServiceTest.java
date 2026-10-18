package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.ClientNotifier;
import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.CancellationNotice;
import com.ticketflow.event_service.domain.CancellationReport;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.EventStatus;
import com.ticketflow.event_service.domain.RefundNotice;
import com.ticketflow.event_service.domain.TicketNotice;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventCancellationServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationEventPublisher eventPublisher;

    private final Event concert = new Event(UUID.randomUUID(), "Concert de jazz", "", "Tunis", EventCategory.CONCERT,
            LocalDateTime.of(2030, 12, 15, 20, 0), 500, 5_000);

    // Fausse transaction : exécute le travail, puis "commite" (on note le moment du commit)
    private final AtomicBoolean committed = new AtomicBoolean(false);
    private final TransactionOperations transaction = new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            T result = action.doInTransaction(null);
            committed.set(true);
            return result;
        }
    };

    // Faux notifier thread-safe : il est appelé EN PARALLÈLE par plusieurs threads virtuels
    private final Set<String> notifiedEmails = ConcurrentHashMap.newKeySet();

    @BeforeEach
    void eventExists() {
        // lenient : le test "événement introuvable" n'utilise pas cette préparation
        lenient().when(eventRepository.findByIdForUpdate(concert.getId())).thenReturn(Optional.of(concert));
    }

    // Les tests ne s'intéressent qu'à l'e-mail d'annulation : une lambda suffit à le simuler
    private EventCancellationService service(Consumer<CancellationNotice> onCancelled) {
        ClientNotifier notifier = new ClientNotifier() {
            @Override public void notifyEventCancelled(CancellationNotice notice) { onCancelled.accept(notice); }
            @Override public void sendTicket(TicketNotice notice) { /* hors sujet ici */ }
            @Override public void notifyRefund(RefundNotice notice) { /* hors sujet ici */ }
        };
        return new EventCancellationService(eventRepository, reservationRepository, eventPublisher, notifier, transaction);
    }

    private Reservation reservation(String email) {
        Reservation reservation = new Reservation(UUID.randomUUID(), concert.getId(), "user", 2);
        reservation.setContactEmail(email);
        return reservation;
    }

    @Test
    void annule_l_evenement_et_ses_reservations_puis_previent_chaque_client() {
        List<Reservation> reservations = List.of(reservation("a@mail.test"), reservation("b@mail.test"));
        when(reservationRepository.findActiveByEventId(concert.getId())).thenReturn(reservations);

        CancellationReport report = service(notice -> notifiedEmails.add(notice.email())).cancelEvent(concert.getId());

        assertThat(concert.getStatus()).isEqualTo(EventStatus.CANCELLED);
        assertThat(reservations).allMatch(r -> r.getStatus() == ReservationStatus.CANCELLED);
        verify(reservationRepository).saveAll(reservations);
        assertThat(notifiedEmails).containsExactlyInAnyOrder("a@mail.test", "b@mail.test");
        assertThat(report.cancelledReservations()).isEqualTo(2);
        assertThat(report.notified()).isEqualTo(2);
        assertThat(report.failed()).isZero();
    }

    @Test
    void les_reservations_deja_payees_sont_remboursees() {
        Reservation paid = reservation("a@mail.test");
        paid.confirm();
        Reservation pending = reservation("b@mail.test");   // rien n'a été débité : rien à rembourser
        when(reservationRepository.findActiveByEventId(concert.getId())).thenReturn(List.of(paid, pending));

        service(notice -> notifiedEmails.add(notice.email())).cancelEvent(concert.getId());

        verify(eventPublisher).publishRefundRequested(paid);
        verify(eventPublisher, never()).publishRefundRequested(pending);
    }

    @Test
    void previent_les_clients_seulement_apres_le_commit_de_la_transaction() {
        when(reservationRepository.findActiveByEventId(concert.getId())).thenReturn(List.of(reservation("a@mail.test")));
        AtomicBoolean committedWhenNotified = new AtomicBoolean(false);

        service(notice -> committedWhenNotified.set(committed.get())).cancelEvent(concert.getId());

        // Aucun e-mail lent pendant la transaction : le verrou sur l'événement est déjà libéré
        assertThat(committedWhenNotified).isTrue();
    }

    @Test
    void un_echec_d_envoi_ne_bloque_pas_les_autres_clients() {
        when(reservationRepository.findActiveByEventId(concert.getId()))
                .thenReturn(List.of(reservation("ok1@mail.test"), reservation("ko@mail.test"), reservation("ok2@mail.test")));
        Consumer<CancellationNotice> notifier = notice -> {
            if (notice.email().startsWith("ko")) {
                throw new IllegalStateException("Boîte mail inexistante");
            }
            notifiedEmails.add(notice.email());
        };

        CancellationReport report = service(notifier).cancelEvent(concert.getId());

        assertThat(notifiedEmails).containsExactlyInAnyOrder("ok1@mail.test", "ok2@mail.test");
        assertThat(report.notified()).isEqualTo(2);
        assertThat(report.failed()).isEqualTo(1);
    }

    @Test
    void compte_les_reservations_sans_email_sans_les_oublier() {
        when(reservationRepository.findActiveByEventId(concert.getId()))
                .thenReturn(List.of(reservation("a@mail.test"), reservation(null)));

        CancellationReport report = service(notice -> notifiedEmails.add(notice.email())).cancelEvent(concert.getId());

        assertThat(report.cancelledReservations()).isEqualTo(2);
        assertThat(report.notified()).isEqualTo(1);
        assertThat(report.withoutEmail()).isEqualTo(1);
    }

    @Test
    void performance_200_clients_a_50_ms_prevenus_en_moins_de_2_secondes() {
        // En séquentiel : 200 × 50 ms = 10 secondes. En parallèle (threads virtuels) : ~50 ms + un peu.
        List<Reservation> reservations = IntStream.range(0, 200).mapToObj(i -> reservation("client" + i + "@mail.test")).toList();
        when(reservationRepository.findActiveByEventId(concert.getId())).thenReturn(reservations);
        Consumer<CancellationNotice> slowNotifier = notice -> {
            try {
                Thread.sleep(50);   // simule l'aller-retour réseau vers le serveur SMTP
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            notifiedEmails.add(notice.email());
        };

        CancellationReport report = service(slowNotifier).cancelEvent(concert.getId());

        assertThat(report.notified()).isEqualTo(200);
        assertThat(notifiedEmails).hasSize(200);
        assertThat(report.duration()).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void evenement_introuvable_aucune_notification() {
        UUID unknown = UUID.randomUUID();
        when(eventRepository.findByIdForUpdate(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(notice -> notifiedEmails.add(notice.email())).cancelEvent(unknown))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(notifiedEmails).isEmpty();
    }
}

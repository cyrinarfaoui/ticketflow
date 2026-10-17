package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.ClientNotifier;
import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.CancellationNotice;
import com.ticketflow.event_service.domain.CancellationReport;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Annulation d'un événement (le concert n'aura pas lieu) : on annule toutes ses réservations et on prévient chaque client.
 *
 * Deux temps, volontairement séparés :
 *  1. Base de données, dans UNE transaction courte : événement verrouillé → CANCELLED, réservations → CANCELLED.
 *  2. Notifications, APRÈS le commit et en parallèle : envoyer des e-mails est lent (réseau) ;
 *     garder la transaction et le verrou ouverts pendant ce temps bloquerait l'événement pour rien (contention).
 */
@Service
public class EventCancellationService {

    private static final Logger log = LoggerFactory.getLogger(EventCancellationService.class);

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final ClientNotifier clientNotifier;
    private final TransactionOperations transaction;   // TransactionTemplate de Spring (transaction "à la main", bornée)

    public EventCancellationService(EventRepository eventRepository, ReservationRepository reservationRepository,
                                    ClientNotifier clientNotifier, TransactionOperations transaction) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.clientNotifier = clientNotifier;
        this.transaction = transaction;
    }

    public CancellationReport cancelEvent(UUID eventId) {
        long start = System.nanoTime();

        // ---- 1. Base de données : une transaction courte ----
        List<CancellationNotice> notices = new ArrayList<>();
        int[] withoutEmail = {0};
        int cancelled = transaction.execute(status -> {
            Event event = eventRepository.findByIdForUpdate(eventId)
                    .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + eventId));
            event.cancel();
            eventRepository.save(event);

            List<Reservation> reservations = reservationRepository.findActiveByEventId(eventId);
            for (Reservation reservation : reservations) {
                reservation.cancel();
                if (reservation.getUserEmail() == null) {
                    withoutEmail[0]++;   // ancienne réservation, sans e-mail : on ne peut pas prévenir ce client
                } else {
                    notices.add(new CancellationNotice(reservation.getUserEmail(), event.getTitle(),
                            event.getDate(), reservation.getSeats()));
                }
            }
            reservationRepository.saveAll(reservations);
            return reservations.size();
        });
        // Ici, la transaction est COMMITÉE et le verrou sur l'événement est libéré.

        // ---- 2. Notifications en parallèle, une tâche par client, chacune sur son thread virtuel ----
        int notified = 0;
        int failed = 0;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> sendings = notices.stream()
                    .<Future<?>>map(notice -> executor.submit(() -> clientNotifier.notifyEventCancelled(notice)))
                    .toList();
            for (Future<?> sending : sendings) {
                try {
                    sending.get();          // attend CE client ; les autres continuent pendant ce temps
                    notified++;
                } catch (ExecutionException e) {
                    failed++;               // un échec n'arrête pas les autres : on le compte et on continue
                    log.warn("Client non prévenu : {}", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Annulation interrompue", e);
                }
            }
        }   // try-with-resources : close() attend la fin de toutes les tâches

        CancellationReport report = new CancellationReport(cancelled, notified, failed, withoutEmail[0],
                Duration.ofNanos(System.nanoTime() - start));
        log.info("Événement {} annulé : {}", eventId, report);
        return report;
    }
}

package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStats;
import com.ticketflow.event_service.domain.ReservationStatus;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher eventPublisher;
    private final Clock clock;

    public ReservationService(EventRepository eventRepository,
                              ReservationRepository reservationRepository,
                              ReservationEventPublisher eventPublisher,
                              Clock clock) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Saga, étape 1 : réserve les places (PENDING) et demande le paiement en publiant reservation.created.
     * paymentMethod est un JETON de moyen de paiement (pm_…), jamais un numéro de carte.
     */
    @Transactional
    public Reservation createReservation(UUID eventId, String userId, int seats, String paymentMethod) {
        // Verrou sur la ligne de l'événement : les réservations simultanées passent une par une
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + eventId));

        event.book(seats);
        eventRepository.save(event);

        Reservation reservation = new Reservation(UUID.randomUUID(), eventId, userId, seats);
        Reservation saved = reservationRepository.save(reservation);

        eventPublisher.publishCreated(saved, event.priceFor(seats), paymentMethod);

        return saved;
    }

    /**
     * Saga, chemin "succès" (payment.completed) : la réservation est confirmée.
     * Idempotent : un message rejoué sur une réservation déjà confirmée ne change rien.
     */
    @Transactional
    public void confirmPayment(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Réservation introuvable : " + reservationId));
        if (!reservation.isPending()) {
            log.info("Paiement confirmé pour la réservation {} déjà {} : rien à faire", reservationId, reservation.getStatus());
            return;
        }
        reservation.confirm();
        reservationRepository.save(reservation);
    }

    /**
     * Saga, chemin "échec" (payment.failed) : COMPENSATION.
     * On défait l'étape 1 : la réservation est annulée avec le motif, et les places sont rendues à l'événement.
     * Idempotent : un message rejoué ne rend pas les places une deuxième fois.
     */
    @Transactional
    public void rejectPayment(UUID reservationId, String declineCode) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Réservation introuvable : " + reservationId));
        if (!reservation.isPending()) {
            log.info("Paiement refusé pour la réservation {} déjà {} : rien à faire", reservationId, reservation.getStatus());
            return;
        }
        reservation.failPayment(declineCode);
        reservationRepository.save(reservation);

        // Verrou : la compensation modifie l'événement, comme une réservation ou une annulation (ticket #16)
        Event event = eventRepository.findByIdForUpdate(reservation.getEventId())
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + reservation.getEventId()));
        event.release(reservation.getSeats());
        eventRepository.save(event);
    }

    /**
     * Annule une réservation.
     * Seul son propriétaire peut le faire, ou un administrateur (dashboard).
     * La vérification est faite ICI, côté serveur : cacher le bouton dans le frontend ne protège rien.
     */
    @Transactional
    public Reservation cancelReservation(UUID reservationId, String userId, boolean admin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Réservation introuvable : " + reservationId));

        if (!admin && !reservation.isOwnedBy(userId)) {
            throw new ReservationAccessDeniedException("Vous ne pouvez annuler que vos propres réservations");
        }

        reservation.cancel();
        reservationRepository.save(reservation);

        Event event = eventRepository.findByIdForUpdate(reservation.getEventId())
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + reservation.getEventId()));
        event.release(reservation.getSeats());
        eventRepository.save(event);

        return reservation;
    }

    public Page<Reservation> listReservations(ReservationStatus status, Pageable pageable) {
        return reservationRepository.findAll(status, pageable);
    }

    /**
     * Les réservations de l'utilisateur, chacune avec son événement.
     * Deux requêtes au total (réservations, puis tous leurs événements d'un coup),
     * et non une requête par réservation : on évite le problème "N+1".
     */
    public List<ReservationWithEvent> listMyReservations(String userId) {
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        if (reservations.isEmpty()) {
            return List.of();
        }

        Set<UUID> eventIds = reservations.stream().map(Reservation::getEventId).collect(Collectors.toSet());
        Map<UUID, Event> events = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        return reservations.stream()
                .map(reservation -> new ReservationWithEvent(reservation, events.get(reservation.getEventId())))
                .toList();
    }

    public ReservationStats stats() {
        // Minuit aujourd'hui, dans le fuseau de l'horloge (Tunis), converti en Instant
        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        return new ReservationStats(
                reservationRepository.countCreatedSince(startOfDay),
                reservationRepository.countByStatus()
        );
    }
}
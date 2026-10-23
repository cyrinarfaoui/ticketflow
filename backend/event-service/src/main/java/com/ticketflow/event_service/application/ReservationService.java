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
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher applicationEvents;
    private final Clock clock;

    public ReservationService(EventRepository eventRepository,
                              ReservationRepository reservationRepository,
                              ReservationEventPublisher eventPublisher,
                              ApplicationEventPublisher applicationEvents,
                              Clock clock) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.applicationEvents = applicationEvents;
        this.clock = clock;
    }

    /**
     * Saga, étape 1 : réserve les places (PENDING) et demande le paiement en publiant reservation.created.
     * paymentMethod est un JETON de moyen de paiement (pm_…), jamais un numéro de carte.
     */
    @Transactional
    public Reservation createReservation(UUID eventId, String userId, String userEmail, int seats, String paymentMethod) {
        // Verrou sur la ligne de l'événement : les réservations simultanées passent une par une
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + eventId));

        event.book(seats);
        eventRepository.save(event);

        Reservation reservation = new Reservation(UUID.randomUUID(), eventId, userId, seats);
        reservation.setContactEmail(userEmail);   // pour pouvoir prévenir le client (annulation de l'événement)
        Reservation saved = reservationRepository.save(reservation);

        eventPublisher.publishCreated(saved, event.priceFor(seats), paymentMethod);

        return saved;
    }

    /**
     * Saga, chemin "succès" (payment.completed) : la réservation est confirmée.
     * Idempotent : un message rejoué sur une réservation déjà confirmée ne change rien.
     * Paiement arrivé TROP TARD (réservation expirée ou annulée entre-temps) : le client a payé sans place,
     * on demande son remboursement.
     */
    @Transactional
    public void confirmPayment(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Réservation introuvable : " + reservationId));
        if (reservation.isConfirmed()) {
            log.info("Paiement confirmé pour la réservation {} déjà confirmée : rien à faire", reservationId);
            return;
        }
        if (!reservation.isPending()) {
            log.warn("Paiement accepté pour la réservation {} déjà {} : remboursement demandé", reservationId, reservation.getStatus());
            eventPublisher.publishRefundRequested(reservation);
            return;
        }
        reservation.confirm();
        reservationRepository.save(reservation);
        // Le billet partira par e-mail APRÈS le commit (ClientMailService, @TransactionalEventListener)
        applicationEvents.publishEvent(new ReservationConfirmed(reservationId));
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
     * Expiration (batch) : le client n'a pas reçu de réponse de paiement dans le délai.
     * Comme une compensation : la réservation expire ET ses places sont rendues à l'événement.
     */
    @Transactional
    public void expireReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Réservation introuvable : " + reservationId));

        // Payée ou annulée entre la lecture du batch et maintenant : on ne touche à rien
        if (!reservation.isPending()) {
            log.info("Réservation {} déjà {} : pas d'expiration", reservationId, reservation.getStatus());
            return;
        }

        reservation.expire();
        reservationRepository.save(reservation);

        // Verrou : on modifie l'événement, comme une réservation ou une compensation
        Event event = eventRepository.findByIdForUpdate(reservation.getEventId())
                .orElseThrow(() -> new NoSuchElementException("Événement introuvable : " + reservation.getEventId()));
        event.release(reservation.getSeats());
        eventRepository.save(event);
    }

    /**
     * Annule une réservation : administrateur uniquement (règle de SecurityConfig sur POST /reservations/{id}/cancel).
     * Un client ne peut pas annuler son billet : seul l'organisateur annule, et rembourse si le billet était payé.
     */
    @Transactional
    public Reservation cancelReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Réservation introuvable : " + reservationId));

        boolean paid = reservation.isConfirmed();
        reservation.cancel();
        reservationRepository.save(reservation);
        if (paid) {
            eventPublisher.publishRefundRequested(reservation);   // places payées puis annulées : on rembourse
        }

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
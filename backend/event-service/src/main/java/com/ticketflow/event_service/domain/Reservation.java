package com.ticketflow.event_service.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Reservation {

    private final UUID id;
    private final UUID eventId;
    // Identifiant Keycloak du propriétaire ("sub" du JWT) ; null pour les réservations d'avant le ticket #10
    private final String userId;
    private final int seats;
    private final Instant createdAt;
    private ReservationStatus status;
    // Code Stripe du refus de paiement (insufficient_funds…), si la réservation a été annulée pour cette raison
    private String paymentFailureCode;
    // E-mail de contact du client (claim "email" du jeton Keycloak), pour le prévenir ; null pour les réservations d'avant le ticket #17
    private String userEmail;

    // Nouvelle réservation : faite maintenant, par un utilisateur identifié (obligatoire)
    public Reservation(UUID id, UUID eventId, String userId, int seats) {
        this(id, eventId, requireUser(userId), seats, Instant.now());
    }

    // Réservation relue depuis la base : on garde ses vraies valeurs
    public Reservation(UUID id, UUID eventId, String userId, int seats, Instant createdAt) {
        if (seats <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être positif");
        }
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.seats = seats;
        this.createdAt = createdAt;
        this.status = ReservationStatus.PENDING;
    }

    public void setContactEmail(String email) {
        this.userEmail = email;
    }

    public boolean isOwnedBy(String userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public void confirm() {
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de confirmer une réservation annulée");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cette réservation est déjà annulée");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * Saga, chemin "échec" : le paiement a été refusé, la réservation est annulée.
     * L'appelant rend ensuite les places à l'événement (compensation).
     */
    public void failPayment(String declineCode) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Seule une réservation en attente peut échouer au paiement");
        }
        this.status = ReservationStatus.CANCELLED;
        this.paymentFailureCode = declineCode;
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public void expire() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Seule une réservation en attente peut expirer");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Une réservation doit appartenir à un utilisateur");
        }
        return userId;
    }
}

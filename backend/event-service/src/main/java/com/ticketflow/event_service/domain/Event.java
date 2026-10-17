package com.ticketflow.event_service.domain;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Event {

    private final UUID id;
    private String title;
    private String description;
    private String location;
    private EventCategory category;
    private LocalDateTime date;
    private int totalSeats;
    private int availableSeats;
    private EventStatus status;
    // Prix d'une place, en CENTIMES (jamais de nombre à virgule pour de l'argent), dans la devise CURRENCY
    private long priceCents;

    // Une seule devise pour toute la billetterie
    public static final String CURRENCY = "TND";

    public Event(UUID id, String title, String description, String location, EventCategory category,
                 LocalDateTime date, int totalSeats, long priceCents) {
        validate(title, location, category, date, totalSeats, priceCents);

        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.category = category;
        this.date = date;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.status = EventStatus.OPEN;
        this.priceCents = priceCents;
    }

    /** Montant à payer pour un nombre de places, en centimes. */
    public long priceFor(int seats) {
        return Math.multiplyExact(priceCents, seats);
    }


    /**
     * Réserve un nombre de places sur l'événement.
     *
     * @param seats nombre de places à réserver, doit être positif
     * @throws IllegalStateException si l'événement est clôturé, ou si le nombre de places
     *         demandées dépasse le nombre de places encore disponibles
     */
    public void book(int seats) {
        if (seats <= 0) {
            throw new IllegalArgumentException("Le nombre de places à réserver doit être positif");
        }
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Cet événement est annulé");
        }
        if (status == EventStatus.CLOSED) {
            throw new IllegalStateException("Les réservations sont clôturées pour cet événement");
        }
        if (seats > availableSeats) {
            throw new IllegalStateException("Pas assez de places disponibles pour cette réservation");
        }
        this.availableSeats -= seats;
    }


    public void release(int seats) {
        if (seats <= 0) {
            throw new IllegalArgumentException("Le nombre de places à libérer doit être positif");
        }
        this.availableSeats = Math.min(totalSeats, availableSeats + seats);
    }

    /**
     * Clôture les réservations (action admin). Les réservations existantes restent valables :
     * on peut encore les annuler, mais plus aucune nouvelle réservation n'est acceptée.
     */
    public void close() {
        if (status != EventStatus.OPEN) {
            throw new IllegalStateException("Seul un événement ouvert peut être clôturé");
        }
        this.status = EventStatus.CLOSED;
    }

    /**
     * Annule l'événement (il n'aura pas lieu). Définitif : plus aucune réservation ni réouverture.
     * Possible même s'il était clôturé : un concert complet peut aussi être annulé.
     */
    public void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Cet événement est déjà annulé");
        }
        this.status = EventStatus.CANCELLED;
    }

    /**
     * Modifie l'événement (back-office admin).
     * Les places déjà réservées sont conservées : on ne peut pas descendre en dessous.
     * Le nouveau prix ne s'applique qu'aux réservations futures (les clients ont déjà payé l'ancien).
     */
    public void update(String title, String description, String location, EventCategory category,
                       LocalDateTime date, int totalSeats, long priceCents) {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Un événement annulé ne peut plus être modifié");
        }
        validate(title, location, category, date, totalSeats, priceCents);
        int booked = bookedSeats();
        if (totalSeats < booked) {
            throw new IllegalStateException("Impossible de descendre sous les " + booked + " place(s) déjà réservée(s)");
        }
        this.title = title;
        this.description = description;
        this.location = location;
        this.category = category;
        this.date = date;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats - booked;   // les clients gardent leurs places
        this.priceCents = priceCents;
    }

    /** Nombre de places déjà réservées. */
    public int bookedSeats() {
        return totalSeats - availableSeats;
    }

    // Règles communes à la création et à la modification (une seule source de vérité)
    private static void validate(String title, String location, EventCategory category, LocalDateTime date,
                                 int totalSeats, long priceCents) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Le titre de l'événement ne peut pas être vide");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Le lieu de l'événement est obligatoire");
        }
        if (category == null) {
            throw new IllegalArgumentException("La catégorie de l'événement est obligatoire");
        }
        if (date == null) {
            throw new IllegalArgumentException("La date de l'événement est obligatoire");
        }
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("Le nombre total de places doit être supérieur à 0");
        }
        if (priceCents <= 0) {
            throw new IllegalArgumentException("Le prix d'une place doit être supérieur à 0");
        }
    }

}

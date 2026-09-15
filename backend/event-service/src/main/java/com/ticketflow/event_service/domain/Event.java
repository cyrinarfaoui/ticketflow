package com.ticketflow.event_service.domain;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Event {

    private final UUID id;
    private final String title;
    private final String description;
    private final LocalDateTime date;
    private final int totalSeats;
    private int availableSeats;

    public Event(UUID id, String title, String description, LocalDateTime date, int totalSeats) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Le titre de l'événement ne peut pas être vide");
        }
        if (date == null) {
            throw new IllegalArgumentException("La date de l'événement est obligatoire");
        }
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("Le nombre total de places doit être supérieur à 0");
        }

        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
    }


    /**
     * Réserve un nombre de places sur l'événement.
     *
     * @param seats nombre de places à réserver, doit être positif
     * @throws IllegalStateException si le nombre de places demandées
     *         dépasse le nombre de places encore disponibles
     */
    public void book(int seats) {
        if (seats <= 0) {
            throw new IllegalArgumentException("Le nombre de places à réserver doit être positif");
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

}

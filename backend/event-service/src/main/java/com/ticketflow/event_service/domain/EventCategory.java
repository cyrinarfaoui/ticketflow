package com.ticketflow.event_service.domain;

/**
 * Catégorie d'un événement. Les valeurs sont stockées telles quelles en base (colonne events.category)
 * et autorisées par la contrainte events_category_check (changeset Liquibase 005).
 */
public enum EventCategory {
    CONCERT,
    THEATRE,
    CONFERENCE,
    SPORT,
    FESTIVAL,
    AUTRE
}

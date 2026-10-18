package com.ticketflow.event_service.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Un message Kafka en attente d'envoi (pattern Outbox). */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String topic;

    // Clé Kafka (id de la réservation) : les messages d'une même réservation restent dans l'ordre
    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    // Le message, en JSON
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // null = pas encore envoyé à Kafka
    @Column(name = "published_at")
    private Instant publishedAt;

    public OutboxEventEntity(String topic, String messageKey, String payload, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}

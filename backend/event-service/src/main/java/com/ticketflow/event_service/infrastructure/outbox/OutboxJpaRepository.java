package com.ticketflow.event_service.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    // Les 100 plus anciens messages pas encore envoyés (requête dérivée du nom de la méthode)
    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}

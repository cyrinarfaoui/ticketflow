package com.ticketflow.event_service.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {
}

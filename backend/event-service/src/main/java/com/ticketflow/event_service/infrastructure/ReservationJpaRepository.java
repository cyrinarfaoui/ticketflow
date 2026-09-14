package com.ticketflow.event_service.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {
}

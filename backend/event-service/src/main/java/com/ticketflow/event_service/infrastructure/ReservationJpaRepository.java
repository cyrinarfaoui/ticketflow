package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    List<ReservationEntity> findByStatusAndCreatedAtBefore(ReservationStatus status, Instant cutoff);
}

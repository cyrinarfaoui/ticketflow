package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    List<ReservationEntity> findByStatusAndCreatedAtBefore(ReservationStatus status, Instant cutoff);

    Page<ReservationEntity> findByStatus(ReservationStatus status, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant since);

    // Projection : Spring Data remplit cette interface avec les colonnes "status" et "total"
    interface StatusCount {
        ReservationStatus getStatus();
        long getTotal();
    }

    @Query("SELECT r.status AS status, COUNT(r) AS total FROM ReservationEntity r GROUP BY r.status")
    List<StatusCount> countByStatus();
}

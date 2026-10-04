package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

    // Fenêtre de dates : début inclus, fin exclue ; les plus proches d'abord
    @Query("SELECT e FROM EventEntity e WHERE " +
            "(:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%'))) AND " +
            "(:location IS NULL OR LOWER(e.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%'))) AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:availableOnly = false OR (e.availableSeats > 0 AND e.status = com.ticketflow.event_service.domain.EventStatus.OPEN)) AND " +
            "e.date >= :start AND e.date < :end " +
            "ORDER BY e.date")
    List<EventEntity> search(@Param("title") String title,
                             @Param("location") String location,
                             @Param("category") EventCategory category,
                             @Param("availableOnly") boolean availableOnly,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);
}

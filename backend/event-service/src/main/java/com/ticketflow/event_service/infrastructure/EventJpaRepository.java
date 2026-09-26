package com.ticketflow.event_service.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

    @Query("SELECT e FROM EventEntity e WHERE " +
            "(:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%'))) AND " +
            "(:location IS NULL OR LOWER(e.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%'))) AND " +
            "(:availableOnly = false OR e.availableSeats > 0)")
    List<EventEntity> search(@Param("title") String title,
                             @Param("location") String location,
                             @Param("availableOnly") boolean availableOnly);
}

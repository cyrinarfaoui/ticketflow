package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    // Back-office : TOUS les événements (passés et annulés compris), filtrés et découpés en pages PAR LA BASE.
    // Pageable ajoute ORDER BY + LIMIT/OFFSET ; Spring génère aussi la requête COUNT pour le nombre total.
    @Query("SELECT e FROM EventEntity e WHERE " +
            "(:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%'))) AND " +
            "(:status IS NULL OR e.status = :status)")
    Page<EventEntity> searchForAdmin(@Param("title") String title,
                                     @Param("status") EventStatus status,
                                     Pageable pageable);

    // SELECT ... FOR UPDATE : la ligne reste verrouillée jusqu'au commit de la transaction.
    // Les autres transactions qui veulent la même ligne attendent leur tour.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventEntity e WHERE e.id = :id")
    Optional<EventEntity> findByIdForUpdate(@Param("id") UUID id);
}

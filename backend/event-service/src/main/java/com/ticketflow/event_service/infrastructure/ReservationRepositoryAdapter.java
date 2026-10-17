package com.ticketflow.event_service.infrastructure;


import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;

    public ReservationRepositoryAdapter(ReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity saved = jpaRepository.save(ReservationMapper.toEntity(reservation));
        return ReservationMapper.toDomain(saved);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return jpaRepository.findById(id).map(ReservationMapper::toDomain);
    }

    @Override
    public Page<Reservation> findAll(ReservationStatus status, Pageable pageable) {
        Page<ReservationEntity> page = (status == null)
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findByStatus(status, pageable);
        return page.map(ReservationMapper::toDomain);
    }

    @Override
    public void saveAll(List<Reservation> reservations) {
        jpaRepository.saveAll(reservations.stream().map(ReservationMapper::toEntity).toList());
    }

    @Override
    public List<Reservation> findActiveByEventId(UUID eventId) {
        return jpaRepository.findByEventIdAndStatusIn(eventId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))
                .stream().map(ReservationMapper::toDomain).toList();
    }

    @Override
    public List<Reservation> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(ReservationMapper::toDomain).toList();
    }

    @Override
    public long countCreatedSince(Instant since) {
        return jpaRepository.countByCreatedAtGreaterThanEqual(since);
    }

    @Override
    public Map<ReservationStatus, Long> countByStatus() {
        return jpaRepository.countByStatus().stream()
                .collect(Collectors.toMap(
                        ReservationJpaRepository.StatusCount::getStatus,
                        ReservationJpaRepository.StatusCount::getTotal));
    }

}

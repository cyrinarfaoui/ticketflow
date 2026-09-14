package com.ticketflow.event_service.infrastructure;


import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Reservation;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

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

}

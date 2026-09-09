package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.domain.Event;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EventRepositoryAdapter implements EventRepository {

    private final EventJpaRepository jpaRepository;

    public EventRepositoryAdapter(EventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Event save(Event event) {
        EventEntity saved = jpaRepository.save(EventMapper.toEntity(event));
        return EventMapper.toDomain(saved);
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return jpaRepository.findById(id).map(EventMapper::toDomain);
    }

    @Override
    public List<Event> findAll() {
        return jpaRepository.findAll().stream().map(EventMapper::toDomain).toList();
    }
}

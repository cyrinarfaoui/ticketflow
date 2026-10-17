package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventStatus;
import com.ticketflow.event_service.domain.EventCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
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

    @Override
    public List<Event> findAllById(Collection<UUID> ids) {
        return jpaRepository.findAllById(ids).stream().map(EventMapper::toDomain).toList();
    }

    @Override
    public List<Event> search(String title, String location, EventCategory category,
                              boolean availableOnly, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.search(title, location, category, availableOnly, start, end).stream().map(EventMapper::toDomain).toList();
    }

    @Override
    public Optional<Event> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(EventMapper::toDomain);
    }

    @Override
    public Page<Event> searchForAdmin(String title, EventStatus status, Pageable pageable) {
        return jpaRepository.searchForAdmin(title, status, pageable).map(EventMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}

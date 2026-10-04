package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.ReservationStats;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    // Il est 10h00 à Tunis (UTC+1) le 4 octobre 2026
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-04T09:00:00Z"), ZoneId.of("Africa/Tunis"));

    @Mock private EventRepository eventRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationEventPublisher eventPublisher;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(eventRepository, reservationRepository, eventPublisher, CLOCK);
    }

    @Test
    void compte_depuis_minuit_heure_de_tunis() {
        // Minuit à Tunis = 23h00 UTC la veille
        Instant minuitTunis = Instant.parse("2026-10-03T23:00:00Z");
        when(reservationRepository.countCreatedSince(minuitTunis)).thenReturn(5L);
        when(reservationRepository.countByStatus()).thenReturn(Map.of(ReservationStatus.EXPIRED, 1L));

        ReservationStats stats = service.stats();

        assertThat(stats.createdToday()).isEqualTo(5);
        assertThat(stats.count(ReservationStatus.EXPIRED)).isEqualTo(1);
    }
}

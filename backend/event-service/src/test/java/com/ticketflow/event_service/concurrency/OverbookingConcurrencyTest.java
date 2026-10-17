package com.ticketflow.event_service.concurrency;

import com.ticketflow.event_service.application.ReservationService;
import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.EventStatus;
import com.ticketflow.event_service.infrastructure.EventEntity;
import com.ticketflow.event_service.infrastructure.EventJpaRepository;
import com.ticketflow.event_service.infrastructure.ReservationEntity;
import com.ticketflow.event_service.infrastructure.ReservationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 50 utilisateurs se ruent sur un événement de 10 places, AU MÊME INSTANT.
 * Pas de @Transactional ici : chaque thread doit vraiment écrire en base, comme en production.
 */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class OverbookingConcurrencyTest {

    private static final int SEATS = 10;
    private static final int USERS = 50;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EventJpaRepository eventRepository;

    @Autowired
    private ReservationJpaRepository reservationRepository;

    // On remplace Kafka par un faux : ce test parle de concurrence, pas de messagerie
    @MockitoBean
    private ReservationEventPublisher eventPublisher;

    private final UUID eventId = UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        // Sans @Transactional, rien n'est annulé automatiquement : on nettoie à la main
        reservationRepository.deleteAll(reservationsOfEvent());
        eventRepository.deleteById(eventId);
    }

    @Test
    void cinquante_utilisateurs_pour_dix_places_ne_provoquent_pas_de_surreservation() throws Exception {
        eventRepository.save(new EventEntity(eventId, "Concert très demandé", "", "Tunis", EventCategory.CONCERT,
                LocalDateTime.now().plusDays(10), SEATS, SEATS, EventStatus.OPEN, 5_000L));

        ExecutorService pool = Executors.newFixedThreadPool(USERS);   // 50 threads
        CountDownLatch startSignal = new CountDownLatch(1);           // le portillon de départ
        AtomicInteger successes = new AtomicInteger();                // compteur sûr entre threads
        List<Future<?>> tasks = new ArrayList<>();

        for (int i = 0; i < USERS; i++) {
            String userId = "user-" + i;
            tasks.add(pool.submit(() -> {
                startSignal.await();                                  // chaque thread attend le départ
                try {
                    reservationService.createReservation(eventId, userId, userId + "@mail.test", 1, "pm_card_visa");
                    successes.incrementAndGet();
                } catch (RuntimeException refused) {
                    // "Pas assez de places" : c'est le comportement attendu pour 40 utilisateurs
                }
                return null;
            }));
        }

        startSignal.countDown();                                      // PARTEZ ! Les 50 threads foncent ensemble
        for (Future<?> task : tasks) {
            task.get(60, TimeUnit.SECONDS);                           // on attend que chacun ait fini
        }
        pool.shutdown();

        int availableSeats = eventRepository.findById(eventId).orElseThrow().getAvailableSeats();
        System.out.printf("Réussites : %d | Réservations en base : %d | Places restantes : %d%n",
                successes.get(), reservationsOfEvent().size(), availableSeats);

        assertThat(reservationsOfEvent()).hasSize(SEATS);             // exactement 10 billets vendus
        assertThat(successes.get()).isEqualTo(SEATS);
        assertThat(availableSeats).isZero();
    }

    private List<ReservationEntity> reservationsOfEvent() {
        return reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getEventId().equals(eventId))
                .toList();
    }
}
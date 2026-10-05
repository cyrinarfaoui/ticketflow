package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationEventPublisher;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStats;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void mes_reservations_sont_accompagnees_de_leur_evenement_en_une_seule_requete() {
        Event concert = event("Concert");
        Event festival = event("Festival");
        Reservation r1 = new Reservation(UUID.randomUUID(), concert.getId(), "user-1", 2);
        Reservation r2 = new Reservation(UUID.randomUUID(), festival.getId(), "user-1", 1);
        Reservation r3 = new Reservation(UUID.randomUUID(), concert.getId(), "user-1", 4);
        when(reservationRepository.findByUserId("user-1")).thenReturn(List.of(r1, r2, r3));
        when(eventRepository.findAllById(Set.of(concert.getId(), festival.getId()))).thenReturn(List.of(concert, festival));

        List<ReservationWithEvent> mine = service.listMyReservations("user-1");

        assertThat(mine).extracting(item -> item.event().getTitle()).containsExactly("Concert", "Festival", "Concert");
        // Un seul appel pour tous les événements (pas un par réservation), et sans doublon d'identifiant
        verify(eventRepository, times(1)).findAllById(anyCollection());
    }

    @Test
    void aucune_requete_sur_les_evenements_quand_l_utilisateur_n_a_pas_de_reservation() {
        when(reservationRepository.findByUserId("user-1")).thenReturn(List.of());

        assertThat(service.listMyReservations("user-1")).isEmpty();
        verifyNoInteractions(eventRepository);
    }

    @Test
    void le_proprietaire_peut_annuler_sa_reservation() {
        Event concert = event("Concert");
        concert.book(2);
        Reservation reservation = new Reservation(UUID.randomUUID(), concert.getId(), "user-1", 2);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(eventRepository.findByIdForUpdate(concert.getId())).thenReturn(Optional.of(concert));

        Reservation cancelled = service.cancelReservation(reservation.getId(), "user-1", false);

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(concert.getAvailableSeats()).isEqualTo(100); // les 2 places sont rendues
    }

    @Test
    void un_autre_utilisateur_ne_peut_pas_annuler_la_reservation() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 2);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancelReservation(reservation.getId(), "pirate", false))
                .isInstanceOf(ReservationAccessDeniedException.class);

        // Rien n'a bougé : ni la réservation, ni les places de l'événement
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void un_administrateur_peut_annuler_la_reservation_de_n_importe_qui() {
        Event concert = event("Concert");
        Reservation reservation = new Reservation(UUID.randomUUID(), concert.getId(), "user-1", 1);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(eventRepository.findByIdForUpdate(concert.getId())).thenReturn(Optional.of(concert));

        Reservation cancelled = service.cancelReservation(reservation.getId(), "admin-42", true);

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    private static Event event(String title) {
        return new Event(UUID.randomUUID(), title, "", "Tunis", EventCategory.CONCERT, LocalDateTime.of(2030, 1, 1, 20, 0), 100);
    }
}

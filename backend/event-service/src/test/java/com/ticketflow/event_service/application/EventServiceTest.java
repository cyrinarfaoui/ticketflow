package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    // "Maintenant" est figé au 3 octobre 2026 à 10h00 (heure de Tunis)
    private static final ZoneId ZONE = ZoneId.of("Africa/Tunis");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T09:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);

    @Mock private ReservationRepository reservationRepository;

    @Mock
    private EventRepository eventRepository;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, reservationRepository, CLOCK);}

    @Test
    void sans_dates_ne_cherche_que_les_evenements_a_venir() {
        service.listEvents(null, null, null, false, null, null);

        verify(eventRepository).search(null, null, null, false, NOW, EventService.NO_END);
    }

    @Test
    void from_dans_le_futur_commence_au_debut_de_cette_journee() {
        service.listEvents(null, null, null, false, LocalDate.of(2026, 10, 10), null);

        verify(eventRepository).search(null, null, null, false, LocalDateTime.of(2026, 10, 10, 0, 0), EventService.NO_END);
    }

    @Test
    void from_dans_le_passe_ne_montre_jamais_les_evenements_passes() {
        service.listEvents(null, null, null, false, LocalDate.of(2026, 9, 1), null);

        verify(eventRepository).search(null, null, null, false, NOW, EventService.NO_END);
    }

    @Test
    void to_inclut_toute_la_derniere_journee() {
        service.listEvents(null, null, null, false, null, LocalDate.of(2026, 10, 20));

        verify(eventRepository).search(null, null, null, false, NOW, LocalDateTime.of(2026, 10, 21, 0, 0));
    }

    @Test
    void refuse_une_date_de_fin_anterieure_a_la_date_de_debut() {
        assertThatThrownBy(() ->
                service.listEvents(null, null, null, false, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date de fin");

        verifyNoInteractions(eventRepository);
    }

    @Test
    void transforme_les_textes_vides_en_absence_de_filtre() {
        service.listEvents("  ", "", null, true, null, null);

        verify(eventRepository).search(null, null, null, true, NOW, EventService.NO_END);
    }

    @Test
    void transmet_la_categorie_demandee() {
        service.listEvents("jazz", "Tunis", EventCategory.CONCERT, false, null, null);

        verify(eventRepository).search("jazz", "Tunis", EventCategory.CONCERT, false, NOW, EventService.NO_END);
    }

    @Test
    void cloture_l_evenement_et_l_enregistre() {
        Event event = new Event(UUID.randomUUID(), "Concert", "", "Tunis", EventCategory.CONCERT, NOW.plusDays(5), 50, 5_000);
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.closeEvent(event.getId());

        // ArgumentCaptor : on récupère l'objet réellement passé à save() pour l'inspecter
        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(EventStatus.CLOSED);
    }

    @Test
    void cloturer_un_evenement_inconnu_renvoie_une_erreur_introuvable() {
        UUID inconnu = UUID.randomUUID();
        when(eventRepository.findByIdForUpdate(inconnu)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.closeEvent(inconnu)).isInstanceOf(NoSuchElementException.class);
    }

    // ---------- Back-office (ticket #19) ----------

    @Test
    void modifier_delegue_les_regles_a_l_evenement_puis_sauvegarde() {
        Event event = new Event(UUID.randomUUID(), "Concert", "", "Tunis", EventCategory.CONCERT, NOW.plusDays(5), 50, 5_000);
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = service.updateEvent(event.getId(), "Concert (nouvelle salle)", "", "Sousse",
                EventCategory.CONCERT, NOW.plusDays(10), 80, 6_000);

        assertThat(updated.getLocation()).isEqualTo("Sousse");
        assertThat(updated.getTotalSeats()).isEqualTo(80);
        verify(eventRepository).save(event);
    }

    @Test
    void refuse_de_modifier_un_evenement_deja_passe() {
        Event past = new Event(UUID.randomUUID(), "Concert", "", "Tunis", EventCategory.CONCERT, NOW.minusDays(1), 50, 5_000);
        when(eventRepository.findByIdForUpdate(past.getId())).thenReturn(Optional.of(past));

        assertThatThrownBy(() -> service.updateEvent(past.getId(), "Concert", "", "Tunis",
                EventCategory.CONCERT, NOW.plusDays(10), 50, 5_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("passé");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void supprime_un_evenement_sans_reservation() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(
                new Event(id, "Concert", "", "Tunis", EventCategory.CONCERT, NOW.plusDays(5), 50, 5_000)));
        when(reservationRepository.existsByEventId(id)).thenReturn(false);

        service.deleteEvent(id);

        verify(eventRepository).deleteById(id);
    }

    @Test
    void refuse_de_supprimer_un_evenement_qui_a_des_reservations() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findByIdForUpdate(id)).thenReturn(Optional.of(
                new Event(id, "Concert", "", "Tunis", EventCategory.CONCERT, NOW.plusDays(5), 50, 5_000)));
        when(reservationRepository.existsByEventId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEvent(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("annulez-le");
        verify(eventRepository, never()).deleteById(any());
    }

    @Test
    void liste_admin_un_titre_vide_veut_dire_pas_de_filtre() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(eventRepository.searchForAdmin(null, null, pageable)).thenReturn(org.springframework.data.domain.Page.empty());

        service.listForAdmin("   ", null, pageable);

        verify(eventRepository).searchForAdmin(null, null, pageable);
    }
}

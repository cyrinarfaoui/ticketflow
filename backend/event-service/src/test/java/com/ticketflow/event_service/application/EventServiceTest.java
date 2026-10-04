package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.EventRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    // "Maintenant" est figé au 3 octobre 2026 à 10h00 (heure de Tunis)
    private static final ZoneId ZONE = ZoneId.of("Africa/Tunis");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T09:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);

    @Mock
    private EventRepository eventRepository;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, CLOCK);
    }

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
        Event event = new Event(UUID.randomUUID(), "Concert", "", "Tunis", EventCategory.CONCERT, NOW.plusDays(5), 50);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
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
        when(eventRepository.findById(inconnu)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.closeEvent(inconnu)).isInstanceOf(NoSuchElementException.class);
    }
}

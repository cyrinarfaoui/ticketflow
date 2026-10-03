package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.EventCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration de la requête de recherche, contre une vraie base Postgres
 * (celle de docker-compose en local, le service postgres:16 en CI).
 * Chaque test est annulé à la fin grâce à @Transactional : la base n'est pas modifiée.
 */
@SpringBootTest
@Transactional
class EventJpaRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 15, 12, 0);
    private static final LocalDateTime NO_END = LocalDateTime.of(9999, 12, 31, 23, 59);

    @Autowired
    private EventJpaRepository repository;

    @BeforeEach
    void setUp() {
        save("Passé", EventCategory.CONCERT, NOW.minusDays(1), 10);
        save("Concert dans 10 jours", EventCategory.CONCERT, NOW.plusDays(10), 10);
        save("Match demain", EventCategory.SPORT, NOW.plusDays(1), 10);
        save("Conférence complète", EventCategory.CONFERENCE, NOW.plusDays(5), 0);
    }

    @Test
    void exclut_les_evenements_passes_et_trie_par_date() {
        List<String> titres = titres(repository.search(null, null, null, false, NOW, NO_END));

        assertThat(titres).containsExactly("Match demain", "Conférence complète", "Concert dans 10 jours");
    }

    @Test
    void respecte_la_date_de_fin_exclue() {
        List<String> titres = titres(repository.search(null, null, null, false, NOW, NOW.plusDays(5)));

        assertThat(titres).containsExactly("Match demain");
    }

    @Test
    void filtre_par_categorie() {
        List<String> titres = titres(repository.search(null, null, EventCategory.CONCERT, false, NOW, NO_END));

        assertThat(titres).containsExactly("Concert dans 10 jours");
    }

    @Test
    void combine_les_filtres_disponibilite_et_texte() {
        List<String> titres = titres(repository.search("CONF", null, null, true, NOW, NO_END));

        assertThat(titres).isEmpty();
    }

    private void save(String title, EventCategory category, LocalDateTime date, int availableSeats) {
        repository.save(new EventEntity(UUID.randomUUID(), title, "", "Tunis", category, date, 10, availableSeats));
    }

    private static List<String> titres(List<EventEntity> events) {
        return events.stream()
                .map(EventEntity::getTitle)
                .filter(t -> !t.isEmpty())
                .toList();
    }
}

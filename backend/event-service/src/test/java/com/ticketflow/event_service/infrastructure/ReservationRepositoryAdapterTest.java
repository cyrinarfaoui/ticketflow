package com.ticketflow.event_service.infrastructure;

import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pagination, tri et filtre des réservations, contre une vraie base Postgres.
 * @Transactional : tout (y compris le deleteAll) est annulé à la fin de chaque test.
 */
@SpringBootTest
@Transactional
class ReservationRepositoryAdapterTest {

    private static final Instant T1 = Instant.parse("2026-10-01T08:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-02T08:00:00Z");
    private static final Instant T3 = Instant.parse("2026-10-03T08:00:00Z");

    // Les plus récentes d'abord
    private static final Sort PAR_DATE_DESC = Sort.by(Sort.Direction.DESC, "createdAt");

    @Autowired
    private ReservationJpaRepository jpaRepository;

    @Autowired
    private ReservationRepositoryAdapter adapter;

    private UUID ancienne, recente, confirmee;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAllInBatch(); // base vide pour ce test (annulé à la fin)
        ancienne = save(ReservationStatus.PENDING, T1);
        confirmee = save(ReservationStatus.CONFIRMED, T2);
        recente = save(ReservationStatus.PENDING, T3);
    }

    @Test
    void decoupe_en_pages_et_donne_le_total() {
        Pageable premierePage = PageRequest.of(0, 2, PAR_DATE_DESC);

        Page<Reservation> page = adapter.findAll(null, premierePage);

        assertThat(page.getContent()).extracting(Reservation::getId).containsExactly(recente, confirmee);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void renvoie_la_page_suivante() {
        Page<Reservation> page = adapter.findAll(null, PageRequest.of(1, 2, PAR_DATE_DESC));

        assertThat(page.getContent()).extracting(Reservation::getId).containsExactly(ancienne);
    }

    @Test
    void filtre_par_statut() {
        Page<Reservation> page = adapter.findAll(ReservationStatus.PENDING, PageRequest.of(0, 10, PAR_DATE_DESC));

        assertThat(page.getContent()).extracting(Reservation::getId).containsExactly(recente, ancienne);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void compte_par_statut() {
        assertThat(adapter.countByStatus())
                .containsEntry(ReservationStatus.PENDING, 2L)
                .containsEntry(ReservationStatus.CONFIRMED, 1L)
                .doesNotContainKey(ReservationStatus.EXPIRED);
    }

    @Test
    void compte_les_reservations_creees_depuis_un_instant() {
        assertThat(adapter.countCreatedSince(T2)).isEqualTo(2); // T2 inclus, T3
    }

    @Test
    void ne_renvoie_que_les_reservations_de_l_utilisateur_les_plus_recentes_d_abord() {
        UUID premiere = saveFor("user-1", T1);
        UUID autreUtilisateur = saveFor("user-2", T2);
        UUID derniere = saveFor("user-1", T3);

        assertThat(adapter.findByUserId("user-1"))
                .extracting(Reservation::getId)
                .containsExactly(derniere, premiere)
                .doesNotContain(autreUtilisateur);
    }

    private UUID saveFor(String userId, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jpaRepository.save(new ReservationEntity(id, UUID.randomUUID(), userId, 1, ReservationStatus.PENDING, createdAt));
        return id;
    }

    private UUID save(ReservationStatus status, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jpaRepository.save(new ReservationEntity(id, UUID.randomUUID(), 1, status, createdAt));
        return id;
    }
}
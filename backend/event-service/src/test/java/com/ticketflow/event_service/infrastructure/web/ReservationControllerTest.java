package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.application.ReservationService;
import com.ticketflow.event_service.application.ReservationWithEvent;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitaire du controller : on vérifie qu'il transmet au service l'utilisateur
 * lu dans le JWT (et son rôle), jamais une valeur venant de la requête.
 */
@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    // Un JWT déjà "vérifié", comme Spring Security le fournit au controller
    private static final Jwt JWT = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user-1")
            .claim("email", "user1@mail.test")
            .build();

    @Mock
    private ReservationService reservationService;

    private ReservationController controller;

    @BeforeEach
    void setUp() {
        controller = new ReservationController(reservationService);
    }

    @Test
    void cree_la_reservation_au_nom_de_l_utilisateur_du_jeton() {
        UUID eventId = UUID.randomUUID();
        Reservation reservation = new Reservation(UUID.randomUUID(), eventId, "user-1", 2);
        when(reservationService.createReservation(eventId, "user-1", "user1@mail.test", 2, "pm_card_visa")).thenReturn(reservation);

        var response = controller.createReservation(eventId, new CreateReservationRequest(2, "pm_card_visa"), JWT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo(reservation.getId());
    }

    @Test
    void mes_reservations_utilise_le_sub_du_jeton() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 1);
        when(reservationService.listMyReservations("user-1"))
                .thenReturn(List.of(new ReservationWithEvent(reservation, null)));

        List<MyReservationResponse> mine = controller.myReservations(JWT);

        assertThat(mine).extracting(MyReservationResponse::id).containsExactly(reservation.getId());
    }

    @Test
    void annulation_delegue_au_service_et_renvoie_la_reservation_annulee() {
        Reservation reservation = cancelled();
        when(reservationService.cancelReservation(reservation.getId())).thenReturn(reservation);

        ReservationResponse response = controller.cancelReservation(reservation.getId());

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    private static Reservation cancelled() {
        Reservation reservation = new Reservation(UUID.randomUUID(), UUID.randomUUID(), "user-1", 1);
        reservation.cancel();
        return reservation;
    }
}

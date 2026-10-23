package com.ticketflow.event_service.infrastructure.web;


import com.ticketflow.event_service.application.ReservationService;
import com.ticketflow.event_service.domain.ReservationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }


    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestParam UUID eventId,
                                                                 @Valid @RequestBody CreateReservationRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        // L'utilisateur vient du JWT vérifié par Spring, JAMAIS du corps de la requête
        var reservation = reservationService.createReservation(
                eventId, jwt.getSubject(), jwt.getClaimAsString("email"), request.seats(), request.paymentMethod());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

    // Réservé à l'administrateur : la règle est dans SecurityConfig (un client reçoit 403)
    @PostMapping("/{id}/cancel")
    public ReservationResponse cancelReservation(@PathVariable UUID id) {
        return ReservationResponse.from(reservationService.cancelReservation(id));
    }

    @GetMapping
    public PageResponse<ReservationResponse> listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(
                reservationService.listReservations(status, pageable).map(ReservationResponse::from)
        );
    }

    // "Mes réservations" : l'utilisateur est celui du jeton, on ne peut pas demander celles d'un autre
    @GetMapping("/me")
    public List<MyReservationResponse> myReservations(@AuthenticationPrincipal Jwt jwt) {
        return reservationService.listMyReservations(jwt.getSubject()).stream()
                .map(MyReservationResponse::from)
                .toList();
    }

    @GetMapping("/stats")
    public ReservationStatsResponse stats() {
        return ReservationStatsResponse.from(reservationService.stats());
    }
}

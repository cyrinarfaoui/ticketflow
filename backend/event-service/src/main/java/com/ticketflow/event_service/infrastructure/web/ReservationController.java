package com.ticketflow.event_service.infrastructure.web;


import com.ticketflow.event_service.application.ReservationService;
import com.ticketflow.event_service.domain.ReservationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }


    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestParam UUID eventId, @Valid @RequestBody CreateReservationRequest request) {
        var reservation = reservationService.createReservation(eventId, request.seats());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

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

    @GetMapping("/stats")
    public ReservationStatsResponse stats() {
        return ReservationStatsResponse.from(reservationService.stats());
    }
}

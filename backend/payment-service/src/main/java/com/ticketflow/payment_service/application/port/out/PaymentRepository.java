package com.ticketflow.payment_service.application.port.out;

import com.ticketflow.payment_service.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    // La clé d'idempotence : au plus UN paiement par réservation
    Optional<Payment> findByReservationId(UUID reservationId);

    Payment save(Payment payment);
}

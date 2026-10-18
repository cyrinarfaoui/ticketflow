package com.ticketflow.payment_service.infrastructure.persistence;

import com.ticketflow.payment_service.application.port.out.PaymentRepository;
import com.ticketflow.payment_service.domain.Money;
import com.ticketflow.payment_service.domain.Payment;
import com.ticketflow.payment_service.domain.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Payment> findByReservationId(UUID reservationId) {
        return jpaRepository.findByReservationId(reservationId).map(PaymentRepositoryAdapter::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        // saveAndFlush : l'écriture part tout de suite en base, une erreur (contrainte…) est connue AVANT la suite
        PaymentEntity saved = jpaRepository.saveAndFlush(new PaymentEntity(
                payment.id(),
                payment.reservationId(),
                payment.amount().amountCents(),
                payment.amount().currency(),
                payment.paymentMethod(),
                payment.result().status(),
                payment.result().declineCode(),
                payment.createdAt()));
        return toDomain(saved);
    }

    static Payment toDomain(PaymentEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getReservationId(),
                new Money(entity.getAmountCents(), entity.getCurrency()),
                entity.getPaymentMethod(),
                new PaymentResult(entity.getStatus(), entity.getDeclineCode()),
                entity.getCreatedAt());
    }
}

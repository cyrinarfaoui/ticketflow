package com.ticketflow.payment_service.infrastructure.persistence;

import com.ticketflow.payment_service.domain.DeclineCode;
import com.ticketflow.payment_service.domain.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    // UNIQUE en base (Liquibase 001) : la garantie ultime d'idempotence
    @Column(name = "reservation_id", nullable = false, unique = true)
    private UUID reservationId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", nullable = false, length = 64)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "decline_code", length = 40)
    private DeclineCode declineCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentEntity() {
        // pour JPA
    }

    public PaymentEntity(UUID id, UUID reservationId, long amountCents, String currency, String paymentMethod,
                         PaymentStatus status, DeclineCode declineCode, Instant createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.declineCode = declineCode;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getReservationId() { return reservationId; }
    public long getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public DeclineCode getDeclineCode() { return declineCode; }
    public Instant getCreatedAt() { return createdAt; }
}

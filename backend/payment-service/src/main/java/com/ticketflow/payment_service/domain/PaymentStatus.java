package com.ticketflow.payment_service.domain;

public enum PaymentStatus {
    SUCCEEDED,
    DECLINED,
    // Accepté puis remboursé (le client a payé sans obtenir de place)
    REFUNDED
}
package com.ticketflow.payment_service.domain;

/** Le résultat d'un paiement : accepté, refusé AVEC son motif, ou remboursé. */
public record PaymentResult(PaymentStatus status, DeclineCode declineCode) {

    public PaymentResult {
        if (status == PaymentStatus.DECLINED && declineCode == null) {
            throw new IllegalArgumentException("Un refus doit toujours avoir un motif");
        }
        if (status != PaymentStatus.DECLINED && declineCode != null) {
            throw new IllegalArgumentException("Seul un refus a un motif de refus");
        }
    }

    public static PaymentResult succeeded() {
        return new PaymentResult(PaymentStatus.SUCCEEDED, null);
    }

    public static PaymentResult refunded() {
        return new PaymentResult(PaymentStatus.REFUNDED, null);
    }

    public static PaymentResult declined(DeclineCode code) {
        return new PaymentResult(PaymentStatus.DECLINED, code);
    }

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }
}
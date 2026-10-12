package com.ticketflow.payment_service.domain;

/** Le résultat d'un paiement : accepté, ou refusé AVEC son motif. */
public record PaymentResult(PaymentStatus status, DeclineCode declineCode) {

    public PaymentResult {
        if (status == PaymentStatus.DECLINED && declineCode == null) {
            throw new IllegalArgumentException("Un refus doit toujours avoir un motif");
        }
        if (status == PaymentStatus.SUCCEEDED && declineCode != null) {
            throw new IllegalArgumentException("Un paiement accepté n'a pas de motif de refus");
        }
    }

    public static PaymentResult succeeded() {
        return new PaymentResult(PaymentStatus.SUCCEEDED, null);
    }

    public static PaymentResult declined(DeclineCode code) {
        return new PaymentResult(PaymentStatus.DECLINED, code);
    }

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }
}
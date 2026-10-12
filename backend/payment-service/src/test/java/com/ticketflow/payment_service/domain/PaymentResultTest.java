package com.ticketflow.payment_service.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentResultTest {

    @Test
    void un_paiement_accepte_n_a_pas_de_motif_de_refus() {
        PaymentResult result = PaymentResult.succeeded();

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.declineCode()).isNull();
        assertThat(result.isSucceeded()).isTrue();
    }

    @Test
    void un_paiement_refuse_porte_toujours_son_motif() {
        PaymentResult result = PaymentResult.declined(DeclineCode.INSUFFICIENT_FUNDS);

        assertThat(result.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(result.declineCode()).isEqualTo(DeclineCode.INSUFFICIENT_FUNDS);
        assertThat(result.isSucceeded()).isFalse();
    }

    @Test
    void un_refus_sans_motif_est_impossible() {
        assertThatThrownBy(() -> new PaymentResult(PaymentStatus.DECLINED, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
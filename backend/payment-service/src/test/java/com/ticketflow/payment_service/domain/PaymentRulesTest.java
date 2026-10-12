package com.ticketflow.payment_service.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRulesTest {

    @Test
    void accepte_un_montant_normal() {
        assertThat(PaymentRules.check(new Money(4_500, "TND"))).isEmpty();          // 45,00 TND
    }

    @Test
    void accepte_exactement_le_plafond() {
        assertThat(PaymentRules.check(new Money(200_000, "TND"))).isEmpty();        // 2 000,00 TND pile
    }

    @Test
    void refuse_un_montant_au_dessus_du_plafond() {
        assertThat(PaymentRules.check(new Money(200_001, "TND")))
                .contains(DeclineCode.AMOUNT_TOO_LARGE);
    }

    @Test
    void refuse_un_montant_nul() {
        assertThat(PaymentRules.check(new Money(0, "TND")))
                .contains(DeclineCode.INVALID_AMOUNT);
    }

    @Test
    void un_montant_negatif_n_existe_pas() {
        assertThatThrownBy(() -> new Money(-100, "TND"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void une_devise_est_un_code_iso_de_3_lettres_majuscules() {
        assertThatThrownBy(() -> new Money(100, "dinar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_code_de_refus_est_celui_de_stripe() {
        assertThat(DeclineCode.INSUFFICIENT_FUNDS.code()).isEqualTo("insufficient_funds");
    }
}
package com.ticketflow.payment_service.infrastructure.provider;

import com.ticketflow.payment_service.domain.DeclineCode;
import com.ticketflow.payment_service.domain.Money;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FakeStripeProviderTest {

    private final FakeStripeProvider provider = new FakeStripeProvider();

    private PaymentResult charge(String paymentMethod) {
        return provider.charge(new PaymentRequest(UUID.randomUUID(), new Money(4_500, "TND"), paymentMethod));
    }

    @Test
    void la_carte_de_test_visa_est_acceptee() {
        assertThat(charge("pm_card_visa").isSucceeded()).isTrue();
    }

    // Un seul test, exécuté une fois par ligne : chaque jeton de test Stripe et le refus attendu
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "pm_card_chargeDeclined,    CARD_DECLINED",
            "pm_card_insufficientFunds, INSUFFICIENT_FUNDS",
            "pm_card_expired,           EXPIRED_CARD",
            "pm_inconnu,                INVALID_PAYMENT_METHOD",
    })
    void chaque_jeton_de_test_donne_son_refus(String paymentMethod, DeclineCode expected) {
        PaymentResult result = charge(paymentMethod);

        assertThat(result.isSucceeded()).isFalse();
        assertThat(result.declineCode()).isEqualTo(expected);
    }
}
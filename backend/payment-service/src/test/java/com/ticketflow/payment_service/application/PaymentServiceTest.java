package com.ticketflow.payment_service.application;

import com.ticketflow.payment_service.application.port.out.PaymentEventPublisher;
import com.ticketflow.payment_service.application.port.out.PaymentProvider;
import com.ticketflow.payment_service.application.port.out.PaymentRepository;
import com.ticketflow.payment_service.domain.DeclineCode;
import com.ticketflow.payment_service.domain.Money;
import com.ticketflow.payment_service.domain.Payment;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;
import com.ticketflow.payment_service.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock private PaymentRepository repository;
    @Mock private PaymentProvider provider;
    @Mock private PaymentEventPublisher publisher;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(repository, provider, publisher, CLOCK);
    }

    private PaymentRequest request(long amountCents, String paymentMethod) {
        return new PaymentRequest(UUID.randomUUID(), new Money(amountCents, "TND"), paymentMethod);
    }

    // save() renvoie le paiement qu'on lui donne, comme le ferait la vraie base
    private void saveReturnsItsArgument() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void un_paiement_accepte_est_enregistre_PUIS_annonce() {
        PaymentRequest request = request(4_500, "pm_card_visa");
        when(repository.findByReservationId(request.reservationId())).thenReturn(Optional.empty());
        when(provider.charge(request)).thenReturn(PaymentResult.succeeded());
        saveReturnsItsArgument();

        Payment payment = service.process(request);

        assertThat(payment.result().status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.reservationId()).isEqualTo(request.reservationId());
        assertThat(payment.createdAt()).isEqualTo(NOW);
        // L'ORDRE compte : on ne prévient les autres qu'une fois le paiement mémorisé
        InOrder order = inOrder(repository, publisher);
        order.verify(repository).save(payment);
        order.verify(publisher).publish(payment);
    }

    @Test
    void un_refus_de_la_banque_est_enregistre_avec_son_motif() {
        PaymentRequest request = request(4_500, "pm_card_insufficientFunds");
        when(repository.findByReservationId(request.reservationId())).thenReturn(Optional.empty());
        when(provider.charge(request)).thenReturn(PaymentResult.declined(DeclineCode.INSUFFICIENT_FUNDS));
        saveReturnsItsArgument();

        Payment payment = service.process(request);

        assertThat(payment.result().declineCode()).isEqualTo(DeclineCode.INSUFFICIENT_FUNDS);
        verify(publisher).publish(payment);
    }

    @Test
    void les_regles_du_commercant_refusent_SANS_solliciter_la_banque() {
        PaymentRequest request = request(200_001, "pm_card_visa");   // au-dessus du plafond
        when(repository.findByReservationId(request.reservationId())).thenReturn(Optional.empty());
        saveReturnsItsArgument();

        Payment payment = service.process(request);

        assertThat(payment.result().declineCode()).isEqualTo(DeclineCode.AMOUNT_TOO_LARGE);
        verifyNoInteractions(provider);   // aucun appel à la banque, donc aucun frais
    }

    @Test
    void un_message_rejoue_ne_debite_pas_deux_fois_mais_republie_le_resultat() {
        PaymentRequest request = request(4_500, "pm_card_visa");
        Payment alreadyDone = Payment.of(request, PaymentResult.succeeded(), NOW.minusSeconds(60));
        when(repository.findByReservationId(request.reservationId())).thenReturn(Optional.of(alreadyDone));

        Payment payment = service.process(request);

        assertThat(payment).isEqualTo(alreadyDone);
        verifyNoInteractions(provider);                // pas de second débit
        verify(repository, never()).save(any());       // pas de second enregistrement
        verify(publisher).publish(alreadyDone);        // mais la saga peut repartir
    }

    // ---------- Remboursement (refund.requested) ----------

    private Payment payment(UUID reservationId, PaymentResult result) {
        return new Payment(UUID.randomUUID(), reservationId, new Money(17_500, "TND"), "pm_card_visa", result, NOW);
    }

    @Test
    void rembourse_un_paiement_accepte() {
        UUID reservationId = UUID.randomUUID();
        Payment paid = payment(reservationId, PaymentResult.succeeded());
        when(repository.findByReservationId(reservationId)).thenReturn(Optional.of(paid));
        saveReturnsItsArgument();

        Payment refunded = service.refund(reservationId);

        assertThat(refunded.result().status()).isEqualTo(PaymentStatus.REFUNDED);
        // D'ABORD enregistré, PUIS remboursé : si l'enregistrement échoue, on n'a pas rendu l'argent
        // (sinon chaque nouvel essai de Kafka rembourserait encore)
        InOrder order = inOrder(repository, provider, publisher);
        order.verify(repository).save(refunded);
        order.verify(provider).refund(paid);
        order.verify(publisher).publishRefunded(refunded);   // event-service préviendra le client par e-mail
    }

    @Test
    void ne_rembourse_jamais_deux_fois() {
        // Message refund.requested livré une 2e fois (Kafka : at-least-once)
        UUID reservationId = UUID.randomUUID();
        when(repository.findByReservationId(reservationId))
                .thenReturn(Optional.of(payment(reservationId, PaymentResult.refunded())));

        service.refund(reservationId);

        verify(provider, never()).refund(any());
        verify(repository, never()).save(any());
    }

    @Test
    void rien_a_rembourser_si_le_paiement_a_ete_refuse() {
        UUID reservationId = UUID.randomUUID();
        when(repository.findByReservationId(reservationId))
                .thenReturn(Optional.of(payment(reservationId, PaymentResult.declined(DeclineCode.CARD_DECLINED))));

        service.refund(reservationId);

        verify(provider, never()).refund(any());
        verify(repository, never()).save(any());
    }

    @Test
    void rien_a_rembourser_si_aucun_paiement() {
        UUID reservationId = UUID.randomUUID();
        when(repository.findByReservationId(reservationId)).thenReturn(Optional.empty());

        assertThat(service.refund(reservationId)).isNull();
        verifyNoInteractions(provider);
    }
}

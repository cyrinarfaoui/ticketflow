package com.ticketflow.payment_service.application;

import com.ticketflow.payment_service.application.port.out.PaymentEventPublisher;
import com.ticketflow.payment_service.application.port.out.PaymentProvider;
import com.ticketflow.payment_service.application.port.out.PaymentRepository;
import com.ticketflow.payment_service.domain.Payment;
import com.ticketflow.payment_service.domain.PaymentRequest;
import com.ticketflow.payment_service.domain.PaymentResult;
import com.ticketflow.payment_service.domain.PaymentRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentProvider provider;
    private final PaymentEventPublisher publisher;
    private final Clock clock;

    public PaymentService(PaymentRepository repository, PaymentProvider provider,
                          PaymentEventPublisher publisher, Clock clock) {
        this.repository = repository;
        this.provider = provider;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public Payment process(PaymentRequest request) {
        // 1. Idempotence : cette réservation a-t-elle DÉJÀ été payée ?
        //    Oui → on rejoue le RÉSULTAT (la saga repart), jamais le PAIEMENT (pas de double débit).
        Optional<Payment> alreadyProcessed = repository.findByReservationId(request.reservationId());
        if (alreadyProcessed.isPresent()) {
            publisher.publish(alreadyProcessed.get());
            return alreadyProcessed.get();
        }

        // 2. Les règles du commerçant d'abord ; la banque seulement si elles passent
        //    (orElseGet : la banque n'est appelée QUE si l'Optional est vide)
        PaymentResult result = PaymentRules.check(request.amount())
                .map(PaymentResult::declined)
                .orElseGet(() -> provider.charge(request));

        // 3. Mémoriser, PUIS annoncer
        Payment payment = repository.save(Payment.of(request, result, Instant.now(clock)));
        publisher.publish(payment);
        return payment;
    }
}

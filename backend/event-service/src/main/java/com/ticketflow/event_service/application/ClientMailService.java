package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.ClientNotifier;
import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.RefundNotice;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.TicketNotice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;
import java.util.UUID;

/**
 * E-mails au client : le billet après paiement, la confirmation de remboursement.
 * Un e-mail qui échoue est journalisé mais ne fait jamais échouer la réservation ni le remboursement :
 * l'argent et les places comptent plus que le message.
 */
@Service
public class ClientMailService {

    private static final Logger log = LoggerFactory.getLogger(ClientMailService.class);

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final ClientNotifier clientNotifier;

    public ClientMailService(ReservationRepository reservationRepository, EventRepository eventRepository,
                             ClientNotifier clientNotifier) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.clientNotifier = clientNotifier;
    }

    /**
     * AFTER_COMMIT : le billet part seulement si la confirmation est bien enregistrée en base.
     * (Envoyé pendant la transaction, un rollback laisserait au client un billet pour une réservation non confirmée,
     * et l'appel SMTP, lent, garderait la transaction ouverte.)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationConfirmed(ReservationConfirmed confirmed) {
        findWithEvent(confirmed.reservationId()).ifPresent(found -> send(confirmed.reservationId(), "billet", () ->
                clientNotifier.sendTicket(new TicketNotice(found.reservation().getUserEmail(), found.event().getTitle(),
                        found.event().getDate(), found.event().getLocation(),
                        found.event().priceFor(found.reservation().getSeats()), Event.CURRENCY,
                        found.reservation().ticketCodes()))));
    }

    /** Le paiement de cette réservation a été remboursé (message payment.refunded). */
    public void notifyRefund(UUID reservationId, long amountCents, String currency) {
        findWithEvent(reservationId).ifPresent(found -> send(reservationId, "remboursement", () ->
                clientNotifier.notifyRefund(new RefundNotice(found.reservation().getUserEmail(), found.event().getTitle(),
                        found.reservation().getSeats(), amountCents, currency))));
    }

    private Optional<ReservationWithEvent> findWithEvent(UUID reservationId) {
        Optional<Reservation> reservation = reservationRepository.findById(reservationId);
        if (reservation.isEmpty() || reservation.get().getUserEmail() == null) {
            log.warn("Réservation {} introuvable ou sans e-mail : client non prévenu", reservationId);
            return Optional.empty();
        }
        return eventRepository.findById(reservation.get().getEventId())
                .map(event -> new ReservationWithEvent(reservation.get(), event));
    }

    private void send(UUID reservationId, String what, Runnable sending) {
        try {
            sending.run();
            log.info("E-mail de {} envoyé pour la réservation {}", what, reservationId);
        } catch (RuntimeException e) {
            log.error("Échec de l'e-mail de {} pour la réservation {}", what, reservationId, e);
        }
    }
}

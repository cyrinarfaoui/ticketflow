package com.ticketflow.event_service.application;

import com.ticketflow.event_service.application.port.out.ClientNotifier;
import com.ticketflow.event_service.application.port.out.EventRepository;
import com.ticketflow.event_service.application.port.out.ReservationRepository;
import com.ticketflow.event_service.domain.Event;
import com.ticketflow.event_service.domain.EventCategory;
import com.ticketflow.event_service.domain.RefundNotice;
import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.TicketNotice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientMailServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private ClientNotifier clientNotifier;

    private ClientMailService service;

    private final Event concert = new Event(UUID.randomUUID(), "Concert de jazz", "", "Tunis", EventCategory.CONCERT,
            LocalDateTime.of(2030, 12, 15, 20, 0), 100, 2_500);
    private final Reservation reservation = new Reservation(UUID.randomUUID(), concert.getId(), "user-1", 2);

    @BeforeEach
    void setUp() {
        service = new ClientMailService(reservationRepository, eventRepository, clientNotifier);
    }

    private void reservationWithEmail() {
        reservation.setContactEmail("client@mail.test");
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(eventRepository.findById(concert.getId())).thenReturn(Optional.of(concert));
    }

    @Test
    void envoie_le_billet_avec_un_code_par_place() {
        reservationWithEmail();

        service.onReservationConfirmed(new ReservationConfirmed(reservation.getId()));

        ArgumentCaptor<TicketNotice> sent = ArgumentCaptor.forClass(TicketNotice.class);
        verify(clientNotifier).sendTicket(sent.capture());
        TicketNotice ticket = sent.getValue();
        assertThat(ticket.email()).isEqualTo("client@mail.test");
        assertThat(ticket.eventTitle()).isEqualTo("Concert de jazz");
        assertThat(ticket.amountCents()).isEqualTo(5_000);   // 2 places × 25,00 TND
        assertThat(ticket.ticketCodes()).containsExactly(
                "TICKETFLOW-" + reservation.getId() + "-1", "TICKETFLOW-" + reservation.getId() + "-2");
    }

    @Test
    void pas_d_email_connu_pas_de_billet() {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        service.onReservationConfirmed(new ReservationConfirmed(reservation.getId()));

        verifyNoInteractions(clientNotifier);
    }

    @Test
    void un_echec_d_envoi_ne_fait_pas_echouer_le_traitement() {
        // La réservation est confirmée et payée : un serveur mail en panne ne doit pas tout faire échouer
        reservationWithEmail();
        doThrow(new MailSendException("SMTP indisponible")).when(clientNotifier).sendTicket(any());

        assertThatCode(() -> service.onReservationConfirmed(new ReservationConfirmed(reservation.getId())))
                .doesNotThrowAnyException();
    }

    @Test
    void previent_le_client_du_remboursement() {
        reservationWithEmail();

        service.notifyRefund(reservation.getId(), 5_000, "TND");

        verify(clientNotifier).notifyRefund(new RefundNotice("client@mail.test", "Concert de jazz", 2, 5_000, "TND"));
    }
}

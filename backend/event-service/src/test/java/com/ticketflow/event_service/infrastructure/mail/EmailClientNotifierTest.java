package com.ticketflow.event_service.infrastructure.mail;

import com.ticketflow.event_service.domain.CancellationNotice;
import com.ticketflow.event_service.domain.RefundNotice;
import com.ticketflow.event_service.domain.TicketNotice;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailClientNotifierTest {

    @Mock
    private JavaMailSender mailSender;

    private final CancellationNotice notice =
            new CancellationNotice("client@mail.test", "Concert de jazz", LocalDateTime.of(2030, 12, 15, 20, 0), 2);

    private EmailClientNotifier notifier() {
        return new EmailClientNotifier(mailSender, new QrCodeGenerator(), "noreply@ticketflow.local");
    }

    @Test
    void envoie_un_email_d_annulation_au_client() {
        notifier().notifyEventCancelled(notice);

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        SimpleMailMessage message = sent.getValue();
        assertThat(message.getTo()).containsExactly("client@mail.test");
        assertThat(message.getFrom()).isEqualTo("noreply@ticketflow.local");
        assertThat(message.getSubject()).isEqualTo("Annulation : Concert de jazz");
        assertThat(message.getText())
                .contains("« Concert de jazz »")
                .contains("dimanche 15 décembre 2030 à 20h00")
                .contains("2 place(s)");
    }

    @Test
    void signale_l_echec_si_le_serveur_mail_refuse() {
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> notifier().notifyEventCancelled(notice))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    void envoie_le_billet_en_html_avec_un_qr_code_par_place() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        TicketNotice ticket = new TicketNotice("client@mail.test", "Concert de jazz", LocalDateTime.of(2030, 12, 15, 20, 0),
                "Tunis", 5_000, "TND", List.of("TICKETFLOW-r1-1", "TICKETFLOW-r1-2"));

        notifier().sendTicket(ticket);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        assertThat(message.getSubject()).isEqualTo("Votre billet : Concert de jazz");
        assertThat(message.getAllRecipients()[0]).hasToString("client@mail.test");
        message.saveChanges();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        message.writeTo(raw);
        String content = raw.toString(StandardCharsets.UTF_8);
        // Les 2 images de QR code sont dans le message, référencées par le HTML
        assertThat(content).contains("Content-ID: <place-1>", "Content-ID: <place-2>", "image/png");
        assertThat(content).doesNotContain("Content-ID: <place-3>");
    }

    @Test
    void previent_le_client_du_remboursement() {
        notifier().notifyRefund(new RefundNotice("client@mail.test", "Concert de jazz", 7, 17_500, "TND"));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo("Remboursement effectué : Concert de jazz");
        assertThat(sent.getValue().getText()).contains("175,00 TND").contains("7 place(s)");
    }
}

package com.ticketflow.event_service.infrastructure.mail;

import com.ticketflow.event_service.domain.CancellationNotice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailClientNotifierTest {

    @Mock
    private JavaMailSender mailSender;

    private final CancellationNotice notice =
            new CancellationNotice("client@mail.test", "Concert de jazz", LocalDateTime.of(2030, 12, 15, 20, 0), 2);

    @Test
    void envoie_un_email_d_annulation_au_client() {
        new EmailClientNotifier(mailSender, "noreply@ticketflow.local").notifyEventCancelled(notice);

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

        assertThatThrownBy(() -> new EmailClientNotifier(mailSender, "noreply@ticketflow.local").notifyEventCancelled(notice))
                .isInstanceOf(MailSendException.class);
    }
}

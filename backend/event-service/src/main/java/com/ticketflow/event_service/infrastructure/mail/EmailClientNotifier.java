package com.ticketflow.event_service.infrastructure.mail;

import com.ticketflow.event_service.application.port.out.ClientNotifier;
import com.ticketflow.event_service.domain.CancellationNotice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Adaptateur e-mail du port ClientNotifier (protocole SMTP).
 * En local, le serveur SMTP est Mailpit (les e-mails sont visibles sur http://localhost:8025, rien ne part sur Internet).
 * En production, on change seulement la configuration spring.mail.* (Brevo, SendGrid, Amazon SES…), pas ce code.
 */
@Component
public class EmailClientNotifier implements ClientNotifier {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailClientNotifier(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void notifyEventCancelled(CancellationNotice notice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notice.email());
        message.setSubject("Annulation : " + notice.eventTitle());
        message.setText("""
                Bonjour,

                Nous sommes désolés : l'événement « %s » prévu le %s est annulé.
                Votre réservation de %d place(s) est annulée et vous serez intégralement remboursé.

                L'équipe TicketFlow
                """.formatted(notice.eventTitle(), DATE.format(notice.eventDate()), notice.seats()));
        mailSender.send(message);   // lève une MailException si le serveur SMTP refuse ou ne répond pas
    }
}

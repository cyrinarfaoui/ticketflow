package com.ticketflow.event_service.infrastructure.mail;

import com.ticketflow.event_service.application.port.out.ClientNotifier;
import com.ticketflow.event_service.domain.CancellationNotice;
import com.ticketflow.event_service.domain.RefundNotice;
import com.ticketflow.event_service.domain.TicketNotice;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final QrCodeGenerator qrCodes;
    private final String from;

    public EmailClientNotifier(JavaMailSender mailSender, QrCodeGenerator qrCodes, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.qrCodes = qrCodes;
        this.from = from;
    }

    @Override
    public void notifyEventCancelled(CancellationNotice notice) {
        sendText(notice.email(), "Annulation : " + notice.eventTitle(), """
                Bonjour,

                Nous sommes désolés : l'événement « %s » prévu le %s est annulé.
                Votre réservation de %d place(s) est annulée et vous serez intégralement remboursé.

                L'équipe TicketFlow
                """.formatted(notice.eventTitle(), DATE.format(notice.eventDate()), notice.seats()));
    }

    @Override
    public void notifyRefund(RefundNotice notice) {
        sendText(notice.email(), "Remboursement effectué : " + notice.eventTitle(), """
                Bonjour,

                Votre paiement de %s pour %d place(s) à « %s » vous a été remboursé.
                Le montant apparaîtra sur votre compte sous quelques jours, selon votre banque.

                L'équipe TicketFlow
                """.formatted(amount(notice.amountCents(), notice.currency()), notice.seats(), notice.eventTitle()));
    }

    /**
     * Le billet : un e-mail HTML avec un QR code par place, intégré dans le message ("inline", cid:place-1…)
     * pour s'afficher directement, sans pièce jointe à ouvrir.
     */
    @Override
    public void sendTicket(TicketNotice notice) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");   // true = plusieurs parties
            helper.setFrom(from);
            helper.setTo(notice.email());
            helper.setSubject("Votre billet : " + notice.eventTitle());
            helper.setText(ticketHtml(notice), true);
            List<String> codes = notice.ticketCodes();
            for (int place = 1; place <= codes.size(); place++) {
                // Le HTML est ajouté AVANT les images : l'ordre exigé par MimeMessageHelper
                helper.addInline("place-" + place, new ByteArrayResource(qrCodes.png(codes.get(place - 1))), "image/png");
            }
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailPreparationException("Billet impossible à préparer", e);
        }
    }

    private String ticketHtml(TicketNotice notice) {
        // htmlEscape : un titre d'événement contenant "<script>" reste du texte, jamais du code
        StringBuilder html = new StringBuilder("""
                <div style="font-family: Arial, sans-serif; color: #111827">
                  <h1 style="font-size: 20px">Votre billet pour « %s »</h1>
                  <p>%s · %s</p>
                  <p>%d place(s) · %s payés</p>
                  <p>Présentez un QR code par personne à l'entrée.</p>
                """.formatted(HtmlUtils.htmlEscape(notice.eventTitle()), DATE.format(notice.eventDate()),
                HtmlUtils.htmlEscape(notice.location()), notice.seats(), amount(notice.amountCents(), notice.currency())));
        for (int place = 1; place <= notice.seats(); place++) {
            html.append("""
                      <div style="display: inline-block; margin: 8px; text-align: center">
                        <img src="cid:place-%d" alt="QR code de la place %d" width="180" height="180"><br>
                        Place %d / %d
                      </div>
                    """.formatted(place, place, place, notice.seats()));
        }
        return html.append("  <p>L'équipe TicketFlow</p>\n</div>").toString();
    }

    private void sendText(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);   // lève une MailException si le serveur SMTP refuse ou ne répond pas
    }

    // 17500 centimes → "175,00 TND"
    private static String amount(long cents, String currency) {
        return String.format(Locale.FRENCH, "%d,%02d %s", cents / 100, cents % 100, currency);
    }
}

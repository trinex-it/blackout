package it.trinex.blackout.service;

import it.trinex.blackout.properties.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    public void sendMail(String to, Context tfContext, String subject) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false);

        helper.setFrom("noreply@trinex.it", mailProperties.getFromName());
        helper.setTo(to);
        helper.setSubject(subject);
//        mimeMessage.setHeader("Content-Type", "text/html; charset=UTF-8");

        String html = templateEngine.process("reset-password-email", tfContext);

        helper.setText(html, true);

        mailSender.send(mimeMessage);
    }
}

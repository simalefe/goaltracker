package com.goaltracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@goaltracker.app}")
    private String fromEmail;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/auth/verify-email?token=" + token;
        String subject = "GoalTracker Pro — E-posta Doğrulama";
        String body = """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>E-posta Adresinizi Doğrulayın</h2>
                <p>Merhaba,</p>
                <p>GoalTracker Pro'ya kayıt olduğunuz için teşekkürler! Aşağıdaki bağlantıya tıklayarak e-posta adresinizi doğrulayın:</p>
                <p><a href="%s" style="background-color: #0d6efd; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px;">E-postamı Doğrula</a></p>
                <p>Bu bağlantı 24 saat geçerlidir.</p>
                <p>Bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
                <hr>
                <p style="color: #888; font-size: 12px;">GoalTracker Pro</p>
            </body>
            </html>
            """.formatted(link);
        sendHtmlEmail(to, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String link = baseUrl + "/auth/reset-password?token=" + token;
        String subject = "GoalTracker Pro — Şifre Sıfırlama";
        String body = """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Şifrenizi Sıfırlayın</h2>
                <p>Merhaba,</p>
                <p>Şifre sıfırlama talebinde bulundunuz. Aşağıdaki bağlantıya tıklayarak yeni şifrenizi belirleyin:</p>
                <p><a href="%s" style="background-color: #0d6efd; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px;">Şifremi Sıfırla</a></p>
                <p>Bu bağlantı 1 saat geçerlidir.</p>
                <p>Bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
                <hr>
                <p style="color: #888; font-size: 12px;">GoalTracker Pro</p>
            </body>
            </html>
            """.formatted(link);
        sendHtmlEmail(to, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("E-posta gönderildi: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("E-posta gönderilemedi: to={}, error={}", to, e.getMessage());
        }
    }
}


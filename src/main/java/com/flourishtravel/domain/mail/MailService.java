package com.flourishtravel.domain.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;

    public MailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.username:}") String username) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = StringUtils.hasText(from) ? from.trim() : (username == null ? "" : username.trim());
    }

    public boolean isEnabled() {
        return enabled && mailSenderProvider.getIfAvailable() != null && StringUtils.hasText(from);
    }

    @Async
    public void sendHtml(String to, String subject, String htmlBody) {
        if (!StringUtils.hasText(to)) {
            log.warn("[Mail] skip: empty recipient, subject={}", subject);
            return;
        }
        if (!enabled) {
            log.info("[Mail] skipped (SMTP off) to={} subject={}", to, subject);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("[Mail] skipped (no JavaMailSender) to={} subject={}", to, subject);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            sender.send(message);
            log.info("[Mail] sent to={} subject={}", to, subject);
        } catch (Exception e) {
            log.warn("[Mail] failed to={} subject={}: {}", to, subject, e.getMessage());
        }
    }

    public static String escape(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

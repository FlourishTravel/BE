package com.flourishtravel.config;

import com.flourishtravel.domain.mail.MailAddresses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * SMTP chỉ tạo khi MAIL_ENABLED=true. Profile cloud vẫn exclude MailSenderAutoConfiguration
 * để app không fail lúc boot nếu chưa cấu hình mail.
 */
@Configuration
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${app.mail.host}") String host,
            @Value("${app.mail.port:587}") int port,
            @Value("${app.mail.username:}") String username,
            @Value("${app.mail.password:}") String password) {
        String user = MailAddresses.extractEmail(username);
        String pass = MailAddresses.stripAppPassword(password);
        if (user.isBlank() || pass.isBlank()) {
            throw new IllegalStateException("MAIL_ENABLED=true nhưng thiếu MAIL_USERNAME hoặc MAIL_PASSWORD");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(user);
        sender.setPassword(pass);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}

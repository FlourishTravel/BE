package com.flourishtravel.domain.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    @Test
    void isEnabledFalseWhenFlagOff() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        MailService service = new MailService(provider, false, "ops@flourish.vn", "ops@flourish.vn");
        assertFalse(service.isEnabled());
        service.sendHtml("guide@example.com", "Hi", "<p>x</p>");
        verify(provider, never()).getIfAvailable();
    }

    @Test
    void sendsWhenEnabledAndSenderPresent() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(mime);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);

        MailService service = new MailService(provider, true, "ops@flourish.vn", "ops@flourish.vn");
        assertTrue(service.isEnabled());
        service.sendHtml("guide@example.com", "Phân công tour", "<p>ok</p>");
        verify(sender).send(mime);
    }

    @Test
    void escapeHtml() {
        assertEquals("&lt;b&gt;x&amp;y&quot;", MailService.escape("<b>x&y\""));
    }

    @Test
    void extractEmailStripsBrackets() {
        assertEquals("khanhtmintel24@gmail.com", MailAddresses.extractEmail("<khanhtmintel24@gmail.com"));
        assertEquals("khanhtmintel24@gmail.com", MailAddresses.extractEmail("Flourish Travel <khanhtmintel24@gmail.com>"));
        assertEquals("abcd1234abcd1234", MailAddresses.stripAppPassword("abcd 1234 abcd 1234"));
    }

    @Test
    void assignedTemplateIncludesLogoAndContact() {
        GuideMailTemplates.Contact contact = new GuideMailTemplates.Contact(
                "https://flourishtravelapp.khanhtn45.id.vn",
                "https://flourishtravelapp.khanhtn45.id.vn/guide/tours",
                "https://flourishtravelapp.khanhtn45.id.vn/help",
                "khanhtmintel24@gmail.com",
                "0901 234 567");
        String html = GuideMailTemplates.assigned("Lan Anh", "Bangkok – Hành trình độc bản", "30/08/2026 – 02/09/2026",
                "Tập trung 06:30 Tân Sơn Nhất", contact);
        assertTrue(html.contains("cid:flourish-logo"));
        assertTrue(html.contains("Flourish Travel"));
        assertTrue(html.contains("khanhtmintel24@gmail.com"));
        assertTrue(html.contains("0901 234 567"));
        assertTrue(html.contains("Tập trung 06:30 Tân Sơn Nhất"));
        assertTrue(html.contains("/guide/tours"));
    }
}

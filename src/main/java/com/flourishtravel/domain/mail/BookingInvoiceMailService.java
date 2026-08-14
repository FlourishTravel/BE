package com.flourishtravel.domain.mail;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class BookingInvoiceMailService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_VI = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_VI = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MailService mailService;
    private final String frontendUrl;
    private final String supportEmail;
    private final String mailUsername;
    private final String hotline;

    public BookingInvoiceMailService(
            MailService mailService,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${app.mail.support-email:}") String supportEmail,
            @Value("${app.mail.username:}") String mailUsername,
            @Value("${app.mail.hotline:}") String hotline) {
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
        this.supportEmail = supportEmail;
        this.mailUsername = mailUsername;
        this.hotline = hotline;
    }

    public void sendAfterCommit(Booking booking, Payment payment, String paymentUrl, boolean paid) {
        User user = booking == null ? null : booking.getUser();
        String to = user == null ? null : user.getEmail();
        if (!StringUtils.hasText(to)) {
            log.warn("[Mail] skip booking invoice: no customer email booking={}",
                    booking == null ? null : booking.getId());
            return;
        }
        BookingInvoiceMailTemplates.InvoiceSnapshot snapshot = snapshot(booking, payment, paymentUrl, paid);
        String subject = paid
                ? "Hóa đơn xác nhận đặt tour " + snapshot.bookingCode() + " — " + snapshot.tourTitle()
                : "Phiếu đặt tour / hóa đơn tạm " + snapshot.bookingCode() + " — " + snapshot.tourTitle();
        String html = BookingInvoiceMailTemplates.build(snapshot);
        runAfterCommit(() -> mailService.sendHtml(to.trim(), "Flourish Travel — " + subject, html));
    }

    BookingInvoiceMailTemplates.InvoiceSnapshot snapshot(Booking booking, Payment payment,
                                                         String paymentUrl, boolean paid) {
        User user = booking.getUser();
        TourSession session = booking.getSession();
        Tour tour = session == null ? null : session.getTour();
        String site = trimSlash(frontendUrl);
        String bookingCode = bookingCode(booking.getId());
        String bookingUrl = site + "/my-journey/booking/" + booking.getId();
        String email = supportEmail != null && !supportEmail.isBlank()
                ? MailAddresses.extractEmail(supportEmail)
                : MailAddresses.extractEmail(mailUsername);

        BigDecimal unit = tour != null && tour.getBasePrice() != null ? tour.getBasePrice() : BigDecimal.ZERO;
        int guests = booking.getGuestCount() == null ? 0 : booking.getGuestCount();
        BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(Math.max(guests, 0)));
        BigDecimal discount = booking.getDiscountAmount() == null ? BigDecimal.ZERO : booking.getDiscountAmount();
        BigDecimal total = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
        BigDecimal paidAmt = paid && payment != null && payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        if (paid) {
            paidAmt = total;
        }
        BigDecimal remaining = total.subtract(paidAmt);
        if (remaining.signum() < 0) {
            remaining = BigDecimal.ZERO;
        }

        Promotion promo = booking.getPromotion();
        Instant issued = booking.getCreatedAt() != null ? booking.getCreatedAt() : Instant.now();

        return new BookingInvoiceMailTemplates.InvoiceSnapshot(
                user != null ? user.getFullName() : "",
                user != null ? user.getEmail() : "",
                user != null ? user.getPhone() : "",
                bookingCode,
                booking.getId() == null ? "" : booking.getId().toString(),
                issued.atZone(ZONE_VN).format(DATE_TIME_VI) + " (GMT+7)",
                paid,
                paid ? "Đã thanh toán đủ" : "Chờ thanh toán",
                providerLabel(payment),
                payment != null ? payment.getOrderId() : bookingCode,
                payment != null ? payment.getProviderTransId() : null,
                paymentUrl,
                tour != null ? tour.getTitle() : "Tour Flourish Travel",
                tour != null ? tour.getDestinationCity() : null,
                duration(tour),
                tour != null && tour.getCategory() != null ? tour.getCategory().getName() : null,
                marketSegment(tour),
                dateRange(session),
                formatDate(session == null ? null : session.getStartDate()),
                formatDate(session == null ? null : session.getEndDate()),
                guests,
                money(unit),
                money(subtotal),
                money(discount),
                money(total),
                money(paidAmt),
                money(remaining),
                promo != null ? promo.getCode() : null,
                booking.getContactPhone(),
                booking.getPickupAddress(),
                booking.getSpecialRequests(),
                booking.getEmergencyContactName(),
                booking.getEmergencyContactPhone(),
                session != null && session.getTourGuide() != null ? session.getTourGuide().getFullName() : null,
                bookingUrl,
                site + "/cancellation-policy",
                site,
                email,
                hotline,
                guestLines(booking),
                itineraryLines(tour),
                clip(tour == null ? null : tour.getHighlightsText(), 900),
                clip(tour == null ? null : tour.getIncludesText(), 900),
                clip(tour == null ? null : tour.getExcludesText(), 900)
        );
    }

    private static List<BookingInvoiceMailTemplates.GuestLine> guestLines(Booking booking) {
        List<BookingGuest> guests = booking.getBookingGuests();
        List<BookingInvoiceMailTemplates.GuestLine> lines = new ArrayList<>();
        if (guests != null && !guests.isEmpty()) {
            guests.stream()
                    .sorted(Comparator.comparing(BookingGuest::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(g -> lines.add(new BookingInvoiceMailTemplates.GuestLine(
                            g.getFullName(),
                            formatDate(g.getDateOfBirth()),
                            documentLabel(g),
                            g.getNationality())));
            return lines;
        }
        if (StringUtils.hasText(booking.getGuestNames())) {
            for (String name : booking.getGuestNames().split(",")) {
                if (name != null && !name.isBlank()) {
                    lines.add(new BookingInvoiceMailTemplates.GuestLine(name.trim(), "—", "Theo hồ sơ đoàn", null));
                }
            }
        }
        return lines;
    }

    private static List<BookingInvoiceMailTemplates.DayLine> itineraryLines(Tour tour) {
        if (tour == null || tour.getItineraries() == null || tour.getItineraries().isEmpty()) {
            return List.of();
        }
        return tour.getItineraries().stream()
                .sorted(Comparator.comparing(TourItinerary::getDayNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(12)
                .map(day -> new BookingInvoiceMailTemplates.DayLine(
                        "Ngày " + (day.getDayNumber() == null ? "" : day.getDayNumber())
                                + (StringUtils.hasText(day.getTitle()) ? " · " + day.getTitle() : ""),
                        firstNonBlank(day.getSummary(), day.getDescription(), day.getHighlights(),
                                day.getAccommodation(), "Theo điều hành đoàn.")))
                .toList();
    }

    private static String documentLabel(BookingGuest g) {
        if (g.getMaskedPassportNumber() != null) {
            return "Hộ chiếu " + g.getMaskedPassportNumber();
        }
        if (g.getMaskedIdNumber() != null) {
            return "CCCD " + g.getMaskedIdNumber();
        }
        return "Mang bản gốc khi tập trung";
    }

    private static String providerLabel(Payment payment) {
        if (payment == null || payment.getProvider() == null) {
            return "Theo cổng thanh toán trên đơn";
        }
        return switch (payment.getProvider().trim().toLowerCase(Locale.ROOT)) {
            case "payos" -> "PayOS";
            case "momo", "ewallet" -> "MoMo";
            case "bank", "bank_transfer" -> "Chuyển khoản ngân hàng";
            case "manual" -> "Ghi nhận thủ công";
            case "card", "credit_card" -> "Thẻ";
            default -> payment.getProvider();
        };
    }

    private static String marketSegment(Tour tour) {
        if (tour == null || tour.getMarketSegment() == null) {
            return null;
        }
        return switch (tour.getMarketSegment().trim().toLowerCase(Locale.ROOT)) {
            case "international" -> "Tour quốc tế";
            case "domestic" -> "Tour nội địa";
            case "school" -> "Tour học sinh";
            case "corporate" -> "Tour đoàn / doanh nghiệp";
            default -> tour.getMarketSegment();
        };
    }

    private static String duration(Tour tour) {
        if (tour == null) return null;
        Integer days = tour.getDurationDays();
        Integer nights = tour.getDurationNights();
        if (days == null && nights == null) return null;
        if (days != null && nights != null) {
            return days + " ngày / " + nights + " đêm";
        }
        if (days != null) return days + " ngày";
        return nights + " đêm";
    }

    private static String dateRange(TourSession session) {
        if (session == null) return "—";
        String start = formatDate(session.getStartDate());
        String end = formatDate(session.getEndDate());
        if (start.equals(end) || "—".equals(end)) return start;
        return start + " – " + end;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_VI);
    }

    private static String money(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        BigDecimal v = amount == null ? BigDecimal.ZERO : amount;
        return nf.format(v) + " VND";
    }

    private static String bookingCode(UUID id) {
        if (id == null) return "FT-PENDING";
        return "FT-" + id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String clip(String raw, int max) {
        if (raw == null || raw.isBlank()) return null;
        String t = raw.replaceAll("\\s+", " ").trim();
        if (t.length() <= max) return t;
        return t.substring(0, max) + "…";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return clip(v, 280);
            }
        }
        return "";
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}

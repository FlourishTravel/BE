package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.tour.entity.TourSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Quy tắc khách được gửi yêu cầu hoàn tiền (hủy sau thanh toán).
 * Chỉ áp dụng khi đơn còn {@code paid} — đơn {@code confirmed} do admin xác nhận, không hủy qua self-service.
 */
@Component
public class BookingRefundEligibility {

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    public boolean canRequestRefund(Booking booking) {
        return refusalReason(booking) == null;
    }

    public String refusalReason(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return "Đơn không hợp lệ";
        }
        String status = booking.getStatus().toLowerCase(Locale.ROOT);
        if (!"paid".equals(status)) {
            return "Chỉ có thể yêu cầu hoàn tiền cho đơn đã thanh toán và chưa được admin xác nhận";
        }
        if (hasPendingRefund(booking)) {
            return "Đã có yêu cầu hoàn tiền đang chờ xử lý";
        }
        TourSession session = booking.getSession();
        if (session == null || session.getStartDate() == null) {
            return "Không xác định được ngày khởi hành";
        }
        LocalDate today = LocalDate.now(ZoneId.of(tourTimezone));
        if (!today.isBefore(session.getStartDate())) {
            return "Không thể hủy/hoàn tiền sau khi tour đã bắt đầu hoặc đã diễn ra";
        }
        return null;
    }

    private boolean hasPendingRefund(Booking booking) {
        if (booking.getRefunds() == null || booking.getRefunds().isEmpty()) {
            return false;
        }
        return booking.getRefunds().stream()
                .map(Refund::getStatus)
                .filter(s -> s != null)
                .anyMatch(s -> "pending".equalsIgnoreCase(s));
    }
}

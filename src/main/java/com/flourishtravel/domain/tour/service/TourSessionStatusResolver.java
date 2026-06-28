package com.flourishtravel.domain.tour.service;

import com.flourishtravel.domain.tour.entity.TourSession;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Suy luận trạng thái session theo lịch khởi hành / kết thúc (múi giờ VN).
 * <ul>
 *   <li>Trước {@code startDate}: scheduled (sắp khởi hành)</li>
 *   <li>Từ {@code startDate} đến {@code endDate} (bao gồm): ongoing (đang diễn ra)</li>
 *   <li>Sau {@code endDate}: completed (đã kết thúc)</li>
 * </ul>
 * {@code cancelled} / {@code completed} do admin hoặc job ghi nhận thì giữ nguyên.
 */
public final class TourSessionStatusResolver {

    public static final String SCHEDULED = "scheduled";
    public static final String ONGOING = "ongoing";
    public static final String COMPLETED = "completed";
    public static final String CANCELLED = "cancelled";

    private TourSessionStatusResolver() {
    }

    public static LocalDate todayInZone(String zoneId) {
        return LocalDate.now(ZoneId.of(zoneId != null && !zoneId.isBlank() ? zoneId : "Asia/Ho_Chi_Minh"));
    }

    /** Trạng thái hiển thị (có thể khác DB nếu job chưa chạy). */
    public static String resolveEffectiveStatus(TourSession session, LocalDate today) {
        if (session == null) {
            return SCHEDULED;
        }
        String stored = normalizeStored(session.getStatus());
        if (CANCELLED.equals(stored) || COMPLETED.equals(stored)) {
            return stored;
        }
        LocalDate start = session.getStartDate();
        if (start == null) {
            return stored;
        }
        LocalDate end = session.getEndDate() != null ? session.getEndDate() : start;
        if (today.isAfter(end)) {
            return COMPLETED;
        }
        if (!today.isBefore(start)) {
            return ONGOING;
        }
        return SCHEDULED;
    }

    /** Trạng thái nên lưu DB (job đồng bộ hàng ngày / khi khởi động). */
    public static String resolveStatusToPersist(TourSession session, LocalDate today) {
        if (session == null) {
            return SCHEDULED;
        }
        if (CANCELLED.equals(normalizeStored(session.getStatus()))) {
            return CANCELLED;
        }
        return switch (resolveEffectiveStatus(session, today)) {
            case COMPLETED -> COMPLETED;
            case ONGOING -> ONGOING;
            default -> SCHEDULED;
        };
    }

    static String normalizeStored(String status) {
        if (status == null || status.isBlank()) {
            return SCHEDULED;
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }
}

package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu trang Quản lý khách (HDV): các booking đã thanh toán của một session,
 * tiến độ điểm danh theo từng người (bảng session_participants: check-in / check-out),
 * kèm ghi chú đơn / điểm đón / khẩn cấp.
 */
@Data
@Builder
public class GuideSessionGuestsDto {

    private UUID sessionId;
    private String tourTitle;
    private String tourCode;
    private LocalDate startDate;
    private LocalDate endDate;

    /** Tổng số dòng người tham gia (lead + booking_guests đã đồng bộ). */
    private int totalGuestSlots;

    /** Số người đã có thời điểm check-in (check_in_at). */
    private int checkedInGuestSlots;

    /** Số người đã check-out (check_out_at). */
    private int checkedOutParticipants;

    /** Số đơn đưa vào danh sách đoàn (paid / confirmed / completed). */
    private int paidBookingCount;

    /** Số đơn có ghi chú / yêu cầu đặc biệt (specialRequests khác rỗng). */
    private int bookingsWithSpecialRequests;

    /**
     * Các điểm trong lịch trình tour (hoạt động có địa điểm / tiêu đề) để điểm danh theo stop.
     */
    private List<ItineraryStopDto> itineraryStops;

    private List<GuideGuestBookingRowDto> bookings;

    @Data
    @Builder
    public static class GuideGuestBookingRowDto {
        private UUID bookingId;
        private UUID travelerUserId;
        private String travelerName;
        private String email;
        private String phone;
        private String avatarUrl;
        private int guestCount;
        private String specialRequests;
        /** contactPhone trên booking nếu có, không thì phone của user. */
        private String effectiveContactPhone;
        private String pickupAddress;
        private String emergencyContactName;
        private String emergencyContactPhone;
        /**
         * Người đặt đã có check-in gathering (session_checkins) hoặc participant LEAD đã check_in_at.
         */
        private boolean checkedInGathering;
        /** Mọi dòng participant của đơn đều đã có check-in. */
        private boolean allParticipantsCheckedIn;
        private List<CompanionLineDto> companions;
        /** Theo dõi từng người — điểm danh / trả khách. */
        private List<ParticipantAttendanceDto> participantAttendance;
    }

    @Data
    @Builder
    public static class ItineraryStopDto {
        private UUID activityId;
        private Integer dayNumber;
        private String dayTitle;
        private Integer sortOrder;
        private String title;
        private String locationName;
        private String activityType;
        private LocalTime startTime;
        private LocalTime endTime;
        /** Số người đã điểm danh tại điểm này (trên session hiện tại). */
        private int checkedInAtStopCount;
    }

    @Data
    @Builder
    public static class ParticipantAttendanceDto {
        private UUID participantId;
        private String rosterKey;
        /** LEAD | COMPANION */
        private String participantRole;
        private String displayName;
        private String phoneSnapshot;
        private int lineIndex;
        /** Điểm danh / trả khách chung cho cả chuyến (session_participants). */
        private Instant checkInAt;
        private Instant checkOutAt;
        /** Theo từng hoạt động trong lịch trình tour. */
        private List<ActivityAttendanceDto> activityAttendance;
    }

    @Data
    @Builder
    public static class ActivityAttendanceDto {
        private UUID activityId;
        private Instant checkInAt;
        private Instant checkOutAt;
    }

    @Data
    @Builder
    public static class CompanionLineDto {
        private String fullName;
        private LocalDate dateOfBirth;
        private String maskedIdNumber;
    }
}

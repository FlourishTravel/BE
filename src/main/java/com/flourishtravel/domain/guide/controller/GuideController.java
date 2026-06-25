package com.flourishtravel.domain.guide.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.guide.dto.GuideSessionDetailDto;
import com.flourishtravel.domain.guide.dto.GuideSessionGuestsDto;
import com.flourishtravel.domain.guide.dto.GuideSessionMemberDto;
import com.flourishtravel.domain.guide.dto.GuideSessionSummaryDto;
import com.flourishtravel.domain.guide.dto.ParticipantActivityAttendanceResultDto;
import com.flourishtravel.domain.guide.dto.SessionCheckinResultDto;
import com.flourishtravel.domain.guide.dto.SessionParticipantResultDto;
import com.flourishtravel.domain.guide.dto.CreateGuideSessionExpenseRequest;
import com.flourishtravel.domain.guide.dto.GuideSessionExpenseDto;
import com.flourishtravel.domain.guide.service.GuideSessionExpenseService;
import com.flourishtravel.domain.guide.service.GuideService;
import com.flourishtravel.domain.tour.dto.SessionActivitySchedulePatchRequest;
import com.flourishtravel.domain.tour.dto.SessionScheduleViewDto;
import com.flourishtravel.domain.tour.service.TourSessionScheduleService;
import com.flourishtravel.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/guide")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;
    private final TourSessionScheduleService sessionScheduleService;
    private final GuideSessionExpenseService expenseService;

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<GuideSessionSummaryDto>>> getMySessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<GuideSessionSummaryDto> sessions = guideService.getMySessions(principal.getId(), year, month, week);
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<GuideSessionDetailDto>> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        GuideSessionDetailDto session = guideService.getSessionById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @GetMapping("/sessions/{sessionId}/members")
    public ResponseEntity<ApiResponse<List<GuideSessionMemberDto>>> getSessionMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<GuideSessionMemberDto> members = guideService.getSessionMembers(sessionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(members));
    }

    /** Booking đã thanh toán + khách kèm đơn, điểm đón, khẩn cấp — cho trang Quản lý khách HDV. */
    @GetMapping("/sessions/{sessionId}/guests")
    public ResponseEntity<ApiResponse<GuideSessionGuestsDto>> getSessionGuests(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        GuideSessionGuestsDto data = guideService.getSessionGuestsBookings(sessionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/checkins")
    public ResponseEntity<ApiResponse<SessionCheckinResultDto>> checkin(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GuideCheckinRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        SessionCheckinResultDto checkin = guideService.checkin(
                principal.getId(),
                request.getSessionId(),
                request.getUserId(),
                request.getCheckInType());
        return ResponseEntity.ok(ApiResponse.ok("Đã check-in", checkin));
    }

    @PostMapping("/sessions/{sessionId}/participants/{participantId}/check-in")
    public ResponseEntity<ApiResponse<SessionParticipantResultDto>> checkInParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID participantId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        SessionParticipantResultDto p = guideService.checkInParticipant(principal.getId(), sessionId, participantId);
        return ResponseEntity.ok(ApiResponse.ok("Đã điểm danh", p));
    }

    @PostMapping("/sessions/{sessionId}/participants/{participantId}/check-out")
    public ResponseEntity<ApiResponse<SessionParticipantResultDto>> checkOutParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID participantId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        SessionParticipantResultDto p = guideService.checkOutParticipant(principal.getId(), sessionId, participantId);
        return ResponseEntity.ok(ApiResponse.ok("Đã check-out", p));
    }

    /** Điểm danh tại một hoạt động / địa điểm trong lịch trình tour. */
    @PostMapping("/sessions/{sessionId}/participants/{participantId}/activities/{activityId}/check-in")
    public ResponseEntity<ApiResponse<ParticipantActivityAttendanceResultDto>> checkInParticipantAtActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID participantId,
            @PathVariable UUID activityId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        ParticipantActivityAttendanceResultDto row = guideService.checkInParticipantAtActivity(
                principal.getId(), sessionId, participantId, activityId);
        return ResponseEntity.ok(ApiResponse.ok("Đã điểm danh tại điểm", row));
    }

    @PostMapping("/sessions/{sessionId}/participants/{participantId}/activities/{activityId}/check-out")
    public ResponseEntity<ApiResponse<ParticipantActivityAttendanceResultDto>> checkOutParticipantAtActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID participantId,
            @PathVariable UUID activityId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        ParticipantActivityAttendanceResultDto row = guideService.checkOutParticipantAtActivity(
                principal.getId(), sessionId, participantId, activityId);
        return ResponseEntity.ok(ApiResponse.ok("Đã check-out tại điểm", row));
    }

    @GetMapping("/sessions/{sessionId}/schedule")
    public ResponseEntity<ApiResponse<SessionScheduleViewDto>> getSessionSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok(sessionScheduleService.getSchedule(sessionId, principal)));
    }

    @PatchMapping("/sessions/{sessionId}/schedule/activities/{activityId}")
    public ResponseEntity<ApiResponse<SessionScheduleViewDto>> patchSessionActivitySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID activityId,
            @RequestBody SessionActivitySchedulePatchRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu bản nháp",
                sessionScheduleService.saveDraft(sessionId, activityId, request, principal)));
    }

    @PostMapping("/sessions/{sessionId}/schedule/activities/{activityId}/publish")
    public ResponseEntity<ApiResponse<SessionScheduleViewDto>> publishSessionActivitySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID activityId) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok("Đã công bố lịch trình",
                sessionScheduleService.publish(sessionId, activityId, principal)));
    }

    @PostMapping("/sessions/{sessionId}/schedule/activities/{activityId}/cancel")
    public ResponseEntity<ApiResponse<SessionScheduleViewDto>> cancelSessionActivitySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID activityId) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy hoạt động trong lịch đoàn",
                sessionScheduleService.cancelActivity(sessionId, activityId, principal)));
    }

    @GetMapping("/sessions/{sessionId}/expenses")
    public ResponseEntity<ApiResponse<List<GuideSessionExpenseDto>>> listSessionExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok(expenseService.listForSession(sessionId, principal.getId())));
    }

    @PostMapping("/sessions/{sessionId}/expenses")
    public ResponseEntity<ApiResponse<GuideSessionExpenseDto>> createSessionExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreateGuideSessionExpenseRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();
        GuideSessionExpenseDto row = expenseService.create(sessionId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đã ghi chi phí", row));
    }

    @DeleteMapping("/sessions/{sessionId}/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteSessionExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID expenseId) {
        if (principal == null) return ResponseEntity.status(401).build();
        expenseService.delete(sessionId, expenseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa chi phí", null));
    }

    @Data
    public static class GuideCheckinRequest {
        private UUID sessionId;
        private UUID userId;
        private String checkInType;
    }
}

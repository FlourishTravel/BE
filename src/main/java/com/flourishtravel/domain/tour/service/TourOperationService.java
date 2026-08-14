package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.mail.GuideMailTemplates;
import com.flourishtravel.domain.mail.MailAddresses;
import com.flourishtravel.domain.mail.MailService;
import com.flourishtravel.domain.notification.service.NotificationService;
import com.flourishtravel.domain.tour.dto.AssignGuideRequest;
import com.flourishtravel.domain.tour.dto.GuideAvailabilityDto;
import com.flourishtravel.domain.tour.dto.SessionStatusRequest;
import com.flourishtravel.domain.tour.dto.TourOperationDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic cho trang Điều hành tour (Tour Operations / Dispatch).
 *
 * Trách nhiệm:
 *  - Lấy danh sách session khởi hành trong khoảng [from, to] để hiển thị calendar/list.
 *  - Lấy danh sách HDV (TOUR_GUIDE active) kèm workload tháng và cờ trùng lịch.
 *  - Gán / huỷ gán HDV cho 1 session.
 *  - Đổi trạng thái session (scheduled | cancelled | completed).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TourOperationService {

    private static final String ROLE_TOUR_GUIDE = "TOUR_GUIDE";
    private static final int URGENT_DAYS = 3;
    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.support-email:}")
    private String supportEmail;

    @Value("${app.mail.hotline:}")
    private String hotline;

    @Value("${app.mail.username:}")
    private String mailUsername;

    private final TourSessionRepository tourSessionRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final NotificationService notificationService;

    // ---------- Queries ----------

    /**
     * Liệt kê session khởi hành trong khoảng cho trang Operations.
     * @param q tuỳ chọn — lọc theo tour title/slug (case-insensitive, partial).
     */
    @Transactional(readOnly = true)
    public List<TourOperationDto> listSessions(LocalDate from, LocalDate to, String q) {
        if (from == null || to == null) {
            throw new BadRequestException("from và to là bắt buộc (YYYY-MM-DD)");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("to phải >= from");
        }

        List<TourSession> sessions = tourSessionRepository.findOperationsBetween(from, to);

        String term = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);

        return sessions.stream()
                .filter(s -> {
                    if (term == null) return true;
                    Tour t = s.getTour();
                    if (t == null) return false;
                    String title = t.getTitle() == null ? "" : t.getTitle().toLowerCase(Locale.ROOT);
                    String slug = t.getSlug() == null ? "" : t.getSlug().toLowerCase(Locale.ROOT);
                    return title.contains(term) || slug.contains(term);
                })
                .map(this::toOperationDto)
                .toList();
    }

    /** Danh sách HDV available (role TOUR_GUIDE, active) kèm workload tháng. */
    @Transactional(readOnly = true)
    public List<GuideAvailabilityDto> listGuides(LocalDate targetDate, UUID excludeSessionId) {
        List<User> guides = userRepository.findActiveByRoleName(ROLE_TOUR_GUIDE);

        YearMonth ym = (targetDate != null) ? YearMonth.from(targetDate) : YearMonth.now();
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        return guides.stream()
                .map(g -> {
                    long count = tourSessionRepository.countAssignedInRange(g.getId(), monthStart, monthEnd);
                    boolean busy = false;
                    if (targetDate != null) {
                        UUID exclude = excludeSessionId != null ? excludeSessionId : new UUID(0L, 0L);
                        busy = tourSessionRepository.existsOverlappingForGuide(
                                g.getId(), exclude, targetDate, targetDate);
                    }
                    return GuideAvailabilityDto.builder()
                            .id(g.getId())
                            .fullName(g.getFullName())
                            .initials(toInitials(g.getFullName()))
                            .email(g.getEmail())
                            .phone(g.getPhone())
                            .avatarUrl(g.getAvatarUrl())
                            .active(Boolean.TRUE.equals(g.getIsActive()))
                            .assignedThisMonth((int) count)
                            .busyOnTargetDate(busy)
                            .workloadLevel(workloadLevel((int) count))
                            .build();
                })
                .sorted(Comparator
                        .comparing(GuideAvailabilityDto::isBusyOnTargetDate)
                        .thenComparingInt(GuideAvailabilityDto::getAssignedThisMonth)
                        .thenComparing(GuideAvailabilityDto::getFullName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    // ---------- Mutations ----------

    /** Gán hoặc đổi HDV cho session. Sẽ chặn nếu HDV trùng lịch session khác. */
    @Transactional
    public TourOperationDto assignGuide(UUID sessionId, AssignGuideRequest req) {
        TourSession session = getSession(sessionId);
        User guide = userRepository.findById(req.getGuideId())
                .orElseThrow(() -> new ResourceNotFoundException("Guide", req.getGuideId()));

        if (guide.getRole() == null || !ROLE_TOUR_GUIDE.equalsIgnoreCase(guide.getRole().getName())) {
            throw new BadRequestException("User được chọn không phải Hướng dẫn viên");
        }
        if (!Boolean.TRUE.equals(guide.getIsActive())) {
            throw new BadRequestException("HDV đang bị tạm khoá, không thể gán");
        }

        LocalDate start = session.getStartDate();
        LocalDate end = session.getEndDate() != null ? session.getEndDate() : session.getStartDate();
        boolean overlap = tourSessionRepository.existsOverlappingForGuide(
                guide.getId(), session.getId(), start, end);
        if (overlap) {
            throw new BadRequestException("HDV đã bận trong khoảng " + start + " - " + end);
        }

        User previous = session.getTourGuide();
        boolean replacing = previous != null && !previous.getId().equals(guide.getId());

        session.setTourGuide(guide);
        TourSession saved = tourSessionRepository.save(session);

        if (Boolean.TRUE.equals(req.getNotify())) {
            String tourTitle = saved.getTour() != null ? saved.getTour().getTitle() : "Tour";
            String note = req.getNote();
            User previousGuide = replacing ? previous : null;
            runAfterCommit(() -> notifyGuideAssignment(guide, previousGuide, sessionId, tourTitle, start, end, note));
        }
        return toOperationDto(saved);
    }

    /** Huỷ gán HDV (đưa session về trạng thái "cần điều phối"). */
    @Transactional
    public TourOperationDto unassignGuide(UUID sessionId) {
        TourSession session = getSession(sessionId);
        session.setTourGuide(null);
        return toOperationDto(tourSessionRepository.save(session));
    }

    /** Đổi trạng thái session (scheduled | cancelled | completed). */
    @Transactional
    public TourOperationDto updateStatus(UUID sessionId, SessionStatusRequest req) {
        TourSession session = getSession(sessionId);
        session.setStatus(req.getStatus().toLowerCase(Locale.ROOT));
        return toOperationDto(tourSessionRepository.save(session));
    }

    // ---------- Helpers ----------

    private void notifyGuideAssignment(User assigned, User previous, UUID sessionId,
                                       String tourTitle, LocalDate start, LocalDate end, String note) {
        try {
            dispatchGuideAssignment(assigned, previous, sessionId, tourTitle, start, end, note);
        } catch (Exception e) {
            log.warn("[Operations] Notify failed for session {}: {}", sessionId, e.getMessage());
        }
    }

    private void dispatchGuideAssignment(User assigned, User previous, UUID sessionId,
                                         String tourTitle, LocalDate start, LocalDate end, String note) {
        String dateRange = formatDateRange(start, end);
        GuideMailTemplates.Contact contact = mailContact();

        if (assigned != null && assigned.getId() != null) {
            String title = "Bạn được phân công dẫn tour";
            String body = tourTitle + " · khởi hành " + dateRange;
            notificationService.createFloraNotification(assigned.getId(), "GUIDE_ASSIGNED", title, body, null);
            mailService.sendHtml(
                    assigned.getEmail(),
                    "Flourish Travel — phân công: " + tourTitle,
                    GuideMailTemplates.assigned(assigned.getFullName(), tourTitle, dateRange, note, contact));
        }

        if (previous != null && previous.getId() != null) {
            String title = "Lịch dẫn tour đã chuyển người khác";
            String body = tourTitle + " · khởi hành " + dateRange + " không còn gán cho bạn.";
            notificationService.createFloraNotification(previous.getId(), "GUIDE_UNASSIGNED", title, body, null);
            mailService.sendHtml(
                    previous.getEmail(),
                    "Flourish Travel — đổi HDV: " + tourTitle,
                    GuideMailTemplates.unassigned(previous.getFullName(), tourTitle, dateRange, contact));
        }
        log.info("[Operations] Notified guide {} for session {} (previous={})",
                assigned != null ? assigned.getEmail() : null,
                sessionId,
                previous != null ? previous.getEmail() : null);
    }

    private GuideMailTemplates.Contact mailContact() {
        String site = trimSlash(frontendUrl);
        String email = StringUtils.hasText(supportEmail)
                ? MailAddresses.extractEmail(supportEmail)
                : MailAddresses.extractEmail(mailUsername);
        return new GuideMailTemplates.Contact(
                site,
                site + "/guide/tours",
                site + "/help",
                email,
                hotline);
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

    private static String formatDateRange(LocalDate start, LocalDate end) {
        if (start == null) return "—";
        if (end == null || end.equals(start)) return start.format(VI_DATE);
        return start.format(VI_DATE) + " – " + end.format(VI_DATE);
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private TourSession getSession(UUID id) {
        return tourSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TourSession", id));
    }

    private TourOperationDto toOperationDto(TourSession s) {
        Tour t = s.getTour();
        int max = s.getMaxParticipants() == null ? 0 : s.getMaxParticipants();
        int curr = s.getCurrentParticipants() == null ? 0 : s.getCurrentParticipants();
        int remaining = Math.max(0, max - curr);
        double occupancy = max > 0 ? (curr * 100.0 / max) : 0.0;
        occupancy = Math.round(occupancy * 10.0) / 10.0;

        User guide = s.getTourGuide();
        boolean hasGuideIssue = (guide == null) || !Boolean.TRUE.equals(guide.getIsActive());

        LocalDate today = TourSessionStatusResolver.todayInZone(tourTimezone);
        boolean isFutureWithin3 = s.getStartDate() != null
                && !s.getStartDate().isBefore(today)
                && !s.getStartDate().isAfter(today.plusDays(URGENT_DAYS));
        boolean urgent = "scheduled".equalsIgnoreCase(s.getStatus())
                && hasGuideIssue
                && isFutureWithin3;

        String issueLevel = "none";
        if (urgent) issueLevel = "critical";
        else if (hasGuideIssue && "scheduled".equalsIgnoreCase(s.getStatus())) issueLevel = "warning";

        String thumb = null;
        if (t != null && t.getImages() != null && !t.getImages().isEmpty()) {
            thumb = t.getImages().stream()
                    .sorted(Comparator.comparing(
                            TourImage::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .findFirst()
                    .map(TourImage::getImageUrl)
                    .orElse(null);
        }

        TourOperationDto.GuideRef guideRef = null;
        if (guide != null) {
            guideRef = TourOperationDto.GuideRef.builder()
                    .id(guide.getId())
                    .fullName(guide.getFullName())
                    .initials(toInitials(guide.getFullName()))
                    .email(guide.getEmail())
                    .phone(guide.getPhone())
                    .avatarUrl(guide.getAvatarUrl())
                    .active(Boolean.TRUE.equals(guide.getIsActive()))
                    .build();
        }

        String code = buildTourCode(t);
        String status = TourSessionStatusResolver.resolveEffectiveStatus(s, today);
        if (TourSessionStatusResolver.SCHEDULED.equals(status) && max > 0 && curr >= max) {
            status = "full";
        }

        return TourOperationDto.builder()
                .sessionId(s.getId())
                .tourId(t != null ? t.getId() : null)
                .tourTitle(t != null ? t.getTitle() : null)
                .tourSlug(t != null ? t.getSlug() : null)
                .tourCode(code)
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(status)
                .issueLevel(issueLevel)
                .maxParticipants(max)
                .currentParticipants(curr)
                .remainingSlots(remaining)
                .occupancyPercent(occupancy)
                .hasGuideIssue(hasGuideIssue)
                .urgent(urgent)
                .tourGuide(guideRef)
                .thumbnailUrl(thumb)
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private String workloadLevel(int count) {
        if (count <= 2) return "light";
        if (count <= 5) return "balanced";
        return "heavy";
    }

    /**
     * Suy tên viết tắt từ full name (lấy chữ đầu tối đa 2 từ).
     * Ví dụ: "Nguyễn Văn Hùng" -> "NH".
     */
    private String toInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            String s = parts[0];
            return s.substring(0, Math.min(2, s.length())).toUpperCase(Locale.ROOT);
        }
        String last = parts[parts.length - 1];
        String first = parts[0];
        return ("" + first.charAt(0) + last.charAt(0)).toUpperCase(Locale.ROOT);
    }

    /**
     * Tạo mã tour ngắn từ slug (uppercase, viết tắt theo dấu '-').
     * "tour-bangkok-ayutthaya-3n" -> "TBA3" (mẹo: lấy chữ cái đầu mỗi token, tối đa 6 ký tự).
     */
    private String buildTourCode(Tour t) {
        if (t == null) return "";
        String slug = t.getSlug();
        if (slug == null || slug.isBlank()) return "";
        String[] tokens = slug.split("-");
        String code = Arrays.stream(tokens)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    if (s.length() == 1) return s;
                    char c0 = s.charAt(0);
                    return String.valueOf(c0);
                })
                .collect(Collectors.joining());
        if (code.length() > 6) code = code.substring(0, 6);
        return code.toUpperCase(Locale.ROOT);
    }
}

package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ForbiddenException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.service.FloraScheduleChangeNotifier;
import com.flourishtravel.domain.guide.service.GuideService;
import com.flourishtravel.domain.tour.SessionScheduleConstants;
import com.flourishtravel.domain.tour.dto.SessionActivitySchedulePatchRequest;
import com.flourishtravel.domain.tour.dto.SessionScheduleViewDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import com.flourishtravel.domain.tour.repository.TourActivityRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.tour.repository.TourSessionActivityOverrideRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.tour.schedule.SessionScheduleMergeHelper;
import com.flourishtravel.domain.tour.schedule.SessionScheduleMergeHelper.EffectiveActivityFields;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourSessionScheduleService {

    private final TourSessionRepository sessionRepository;
    private final TourRepository tourRepository;
    private final TourActivityRepository activityRepository;
    private final TourSessionActivityOverrideRepository overrideRepository;
    private final UserRepository userRepository;
    private final GuideService guideService;
    private final FloraScheduleChangeNotifier scheduleChangeNotifier;

    @Transactional(readOnly = true)
    public SessionScheduleViewDto getSchedule(UUID sessionId, UserPrincipal principal) {
        TourSession session = requireManageableSession(sessionId, principal);
        return buildScheduleView(session, true);
    }

    @Transactional(readOnly = true)
    public Map<UUID, TourSessionActivityOverride> loadPublishedOverrides(UUID sessionId) {
        return indexPublished(overrideRepository.findByTourSession_Id(sessionId));
    }

    @Transactional
    public SessionScheduleViewDto saveDraft(
            UUID sessionId,
            UUID activityId,
            SessionActivitySchedulePatchRequest request,
            UserPrincipal principal) {
        TourSession session = requireManageableSession(sessionId, principal);
        TourActivity activity = requireActivityForSession(session, activityId);
        validatePatch(request, activity, session);

        TourSessionActivityOverride override = overrideRepository
                .findByTourSession_IdAndTourActivity_Id(sessionId, activityId)
                .orElseGet(() -> TourSessionActivityOverride.builder()
                        .tourSession(session)
                        .tourActivity(activity)
                        .publicationStatus(SessionScheduleConstants.PUBLICATION_DRAFT)
                        .version(0)
                        .build());

        if (SessionScheduleConstants.PUBLICATION_PUBLISHED.equalsIgnoreCase(override.getPublicationStatus())) {
            override.setPublicationStatus(SessionScheduleConstants.PUBLICATION_DRAFT);
        }

        applyPatch(override, request);
        if (!SessionScheduleConstants.PUBLICATION_CANCELLED.equalsIgnoreCase(override.getPublicationStatus())) {
            override.setPublicationStatus(SessionScheduleConstants.PUBLICATION_DRAFT);
        }
        overrideRepository.save(override);
        return buildScheduleView(session, true);
    }

    @Transactional
    public SessionScheduleViewDto publish(
            UUID sessionId,
            UUID activityId,
            UserPrincipal principal) {
        TourSession session = requireManageableSession(sessionId, principal);
        TourActivity activity = requireActivityForSession(session, activityId);

        TourSessionActivityOverride override = overrideRepository
                .findByTourSession_IdAndTourActivity_Id(sessionId, activityId)
                .orElseThrow(() -> new BadRequestException("Chưa có bản nháp để công bố"));

        if (SessionScheduleConstants.PUBLICATION_DRAFT.equalsIgnoreCase(override.getPublicationStatus())) {
            validatePublishable(override, activity);
        }

        EffectiveActivityFields before = SessionScheduleMergeHelper.resolve(activity,
                findPublishedOverride(sessionId, activityId));
        EffectiveActivityFields afterPreview = SessionScheduleMergeHelper.resolveDraftPreview(activity, override);

        User publisher = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        boolean material = FloraScheduleChangeNotifier.isMaterialChange(before, afterPreview);
        boolean alreadyPublished = SessionScheduleConstants.PUBLICATION_PUBLISHED
                .equalsIgnoreCase(override.getPublicationStatus());

        if (!material && alreadyPublished) {
            return buildScheduleView(session, true);
        }

        override.setPublicationStatus(SessionScheduleConstants.PUBLICATION_PUBLISHED);
        if (material) {
            override.setVersion(override.getVersion() + 1);
            override.setPublishedAt(java.time.Instant.now());
            override.setPublishedBy(publisher);
        }
        overrideRepository.save(override);

        if (material) {
            LocalDate activityDate = resolveActivityDate(session, activity);
            scheduleChangeNotifier.notifyPublishedOverride(session, override, before, afterPreview, activityDate);
        }

        return buildScheduleView(session, true);
    }

    @Transactional
    public SessionScheduleViewDto cancelActivity(
            UUID sessionId,
            UUID activityId,
            UserPrincipal principal) {
        TourSession session = requireManageableSession(sessionId, principal);
        TourActivity activity = requireActivityForSession(session, activityId);

        EffectiveActivityFields before = SessionScheduleMergeHelper.resolve(activity,
                findPublishedOverride(sessionId, activityId));

        TourSessionActivityOverride override = overrideRepository
                .findByTourSession_IdAndTourActivity_Id(sessionId, activityId)
                .orElseGet(() -> TourSessionActivityOverride.builder()
                        .tourSession(session)
                        .tourActivity(activity)
                        .version(0)
                        .build());

        override.setPublicationStatus(SessionScheduleConstants.PUBLICATION_CANCELLED);
        override.setScheduleStatus(FloraScheduleConstants.SCHEDULE_UNAVAILABLE);
        override.setVersion(override.getVersion() + 1);
        override.setPublishedAt(java.time.Instant.now());
        override.setPublishedBy(userRepository.findById(principal.getId()).orElse(null));
        overrideRepository.save(override);

        EffectiveActivityFields after = SessionScheduleMergeHelper.resolve(activity, override);
        LocalDate activityDate = resolveActivityDate(session, activity);
        scheduleChangeNotifier.notifyPublishedOverride(session, override, before, after, activityDate);

        return buildScheduleView(session, true);
    }

    private LocalDate resolveActivityDate(TourSession session, TourActivity activity) {
        if (session.getStartDate() == null) return null;
        Tour tour = session.getTour();
        if (tour == null) return session.getStartDate();
        return tourRepository.findByIdWithItinerariesAndActivities(tour.getId())
                .map(t -> {
                    if (t.getItineraries() == null) return session.getStartDate();
                    for (TourItinerary day : t.getItineraries()) {
                        if (day.getActivities() == null) continue;
                        for (TourActivity act : day.getActivities()) {
                            if (act.getId().equals(activity.getId())) {
                                int dayNum = day.getDayNumber() != null ? day.getDayNumber() : 1;
                                return session.getStartDate().plusDays(Math.max(0, dayNum - 1L));
                            }
                        }
                    }
                    return session.getStartDate();
                })
                .orElse(session.getStartDate());
    }

    private TourSessionActivityOverride findPublishedOverride(UUID sessionId, UUID activityId) {
        return overrideRepository.findByTourSession_IdAndTourActivity_Id(sessionId, activityId)
                .filter(o -> SessionScheduleConstants.PUBLICATION_PUBLISHED.equalsIgnoreCase(o.getPublicationStatus()))
                .orElse(null);
    }

    private TourSession requireManageableSession(UUID sessionId, UserPrincipal principal) {
        if (principal == null) throw new ForbiddenException("Yêu cầu đăng nhập");
        if (isAdmin(principal)) {
            return sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("TourSession", sessionId));
        }
        try {
            guideService.getSessionById(sessionId, principal.getId());
        } catch (BadRequestException ex) {
            throw new ForbiddenException(ex.getMessage());
        }
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("TourSession", sessionId));
    }

    private TourActivity requireActivityForSession(TourSession session, UUID activityId) {
        Tour tour = session.getTour();
        if (tour == null || !activityRepository.existsForTour(activityId, tour.getId())) {
            throw new BadRequestException("Hoạt động không thuộc tour của chuyến này");
        }
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("TourActivity", activityId));
    }

    private static boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a));
    }

    private void applyPatch(TourSessionActivityOverride override, SessionActivitySchedulePatchRequest req) {
        if (req == null) return;
        if (req.getTitle() != null) override.setTitleOverride(trimOrNull(req.getTitle()));
        if (req.getDescription() != null) override.setDescriptionOverride(trimOrNull(req.getDescription()));
        if (req.getStartAt() != null) override.setStartTimeOverride(req.getStartAt().toLocalTime());
        if (req.getEndAt() != null) override.setEndTimeOverride(req.getEndAt().toLocalTime());
        if (req.getLocationName() != null) override.setLocationNameOverride(trimOrNull(req.getLocationName()));
        if (req.getLocationAddress() != null) override.setLocationAddressOverride(trimOrNull(req.getLocationAddress()));
        if (req.getLatitude() != null) override.setLatitudeOverride(req.getLatitude());
        if (req.getLongitude() != null) override.setLongitudeOverride(req.getLongitude());
        if (req.getIsGatheringEvent() != null) override.setIsGatheringEventOverride(req.getIsGatheringEvent());
        if (req.getGatheringEventType() != null) {
            override.setGatheringEventTypeOverride(req.getGatheringEventType().trim().toUpperCase(Locale.ROOT));
        }
        if (req.getScheduleStatus() != null) {
            override.setScheduleStatus(req.getScheduleStatus().trim().toUpperCase(Locale.ROOT));
        }
        if (req.getOperationalNote() != null) override.setOperationalNote(trimOrNull(req.getOperationalNote()));
    }

    private void validatePatch(SessionActivitySchedulePatchRequest req, TourActivity activity, TourSession session) {
        if (req == null) return;
        LocalTime start = req.getStartAt() != null ? req.getStartAt().toLocalTime() : activity.getStartTime();
        LocalTime end = req.getEndAt() != null ? req.getEndAt().toLocalTime() : activity.getEndTime();
        if (start != null && end != null && !start.isBefore(end)) {
            throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc");
        }
        validateCoordinates(req.getLatitude(), req.getLongitude());
        if (req.getScheduleStatus() != null) {
            validateScheduleStatus(req.getScheduleStatus());
        }
    }

    private void validatePublishable(TourSessionActivityOverride override, TourActivity activity) {
        EffectiveActivityFields preview = SessionScheduleMergeHelper.resolveDraftPreview(activity, override);
        String status = preview.getScheduleStatus();
        if (status != null) validateScheduleStatus(status);

        if (preview.isGatheringEvent()
                && FloraScheduleConstants.SCHEDULE_CONFIRMED.equalsIgnoreCase(status != null ? status : "")) {
            if (preview.getStartTime() == null
                    || preview.getLocationName() == null
                    || preview.getLocationName().isBlank()) {
                throw new BadRequestException("Sự kiện tập trung CONFIRMED cần giờ và địa điểm trước khi công bố");
            }
        }
    }

    private static void validateScheduleStatus(String status) {
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (!FloraScheduleConstants.SCHEDULE_CONFIRMED.equals(s)
                && !FloraScheduleConstants.SCHEDULE_ESTIMATED.equals(s)
                && !FloraScheduleConstants.SCHEDULE_UNAVAILABLE.equals(s)) {
            throw new BadRequestException("scheduleStatus không hợp lệ");
        }
    }

    private static void validateCoordinates(BigDecimal lat, BigDecimal lon) {
        if (lat == null && lon == null) return;
        if (lat == null || lon == null) {
            throw new BadRequestException("Cần cả latitude và longitude");
        }
        double la = lat.doubleValue();
        double lo = lon.doubleValue();
        if (la < -90 || la > 90 || lo < -180 || lo > 180) {
            throw new BadRequestException("Toạ độ không hợp lệ");
        }
    }

    private SessionScheduleViewDto buildScheduleView(TourSession session, boolean includeDraft) {
        Tour tour = session.getTour();
        List<TourItinerary> days = List.of();
        if (tour != null) {
            days = tourRepository.findByIdWithItinerariesAndActivities(tour.getId())
                    .map(t -> t.getItineraries() != null ? t.getItineraries() : List.<TourItinerary>of())
                    .orElse(List.of());
        }

        Map<UUID, TourSessionActivityOverride> overrides = new HashMap<>();
        for (TourSessionActivityOverride o : overrideRepository.findByTourSession_Id(session.getId())) {
            overrides.put(o.getTourActivity().getId(), o);
        }

        List<SessionScheduleViewDto.SessionScheduleDayDto> dayDtos = days.stream()
                .sorted(Comparator.comparing(TourItinerary::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(day -> SessionScheduleViewDto.SessionScheduleDayDto.builder()
                        .dayNumber(day.getDayNumber())
                        .title(day.getTitle())
                        .activities(buildActivityRows(day, session, overrides, includeDraft))
                        .build())
                .toList();

        return SessionScheduleViewDto.builder()
                .sessionId(session.getId())
                .tourId(tour != null ? tour.getId() : null)
                .days(dayDtos)
                .build();
    }

    private List<SessionScheduleViewDto.SessionScheduleActivityDto> buildActivityRows(
            TourItinerary day,
            TourSession session,
            Map<UUID, TourSessionActivityOverride> overrides,
            boolean includeDraft) {

        List<TourActivity> activities = day.getActivities() == null ? List.of() :
                day.getActivities().stream()
                        .sorted(Comparator.comparing(TourActivity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();

        return activities.stream().map(act -> {
            TourSessionActivityOverride raw = overrides.get(act.getId());
            TourSessionActivityOverride published = SessionScheduleMergeHelper.isPublishedForTravelers(raw) ? raw : null;
            EffectiveActivityFields effective = SessionScheduleMergeHelper.resolve(act, published);

            SessionScheduleViewDto.OverrideActivityDto overrideDto = null;
            if (raw != null && (includeDraft || SessionScheduleMergeHelper.isPublishedForTravelers(raw)
                    || SessionScheduleConstants.PUBLICATION_CANCELLED.equalsIgnoreCase(raw.getPublicationStatus()))) {
                overrideDto = SessionScheduleViewDto.OverrideActivityDto.builder()
                        .publicationStatus(raw.getPublicationStatus())
                        .title(raw.getTitleOverride())
                        .startTime(raw.getStartTimeOverride())
                        .endTime(raw.getEndTimeOverride())
                        .locationName(raw.getLocationNameOverride())
                        .locationAddress(raw.getLocationAddressOverride())
                        .isGatheringEvent(raw.getIsGatheringEventOverride())
                        .gatheringEventType(raw.getGatheringEventTypeOverride())
                        .scheduleStatus(raw.getScheduleStatus())
                        .operationalNote(raw.getOperationalNote())
                        .version(raw.getVersion())
                        .publishedAt(raw.getPublishedAt())
                        .publishedByName(raw.getPublishedBy() != null ? raw.getPublishedBy().getFullName() : null)
                        .build();
            }

            String sourceLabel = effective.isFromPublishedOverride() ? "Bản cập nhật của đoàn"
                    : (effective.isCancelled() ? "Đã hủy" : "Lịch mẫu");

            return SessionScheduleViewDto.SessionScheduleActivityDto.builder()
                    .activityId(act.getId())
                    .template(templateDto(act))
                    .override(overrideDto)
                    .effective(SessionScheduleViewDto.EffectiveActivityDto.builder()
                            .title(effective.getTitle())
                            .startTime(effective.getStartTime())
                            .endTime(effective.getEndTime())
                            .locationName(effective.getLocationName())
                            .locationAddress(effective.getLocationAddress())
                            .scheduleStatus(effective.getScheduleStatus())
                            .isGatheringEvent(effective.isGatheringEvent())
                            .gatheringEventType(effective.getGatheringEventType())
                            .cancelled(effective.isCancelled())
                            .build())
                    .sourceLabel(sourceLabel)
                    .build();
        }).toList();
    }

    private static SessionScheduleViewDto.TemplateActivityDto templateDto(TourActivity act) {
        return SessionScheduleViewDto.TemplateActivityDto.builder()
                .title(act.getTitle())
                .startTime(act.getStartTime())
                .endTime(act.getEndTime())
                .locationName(act.getLocationName())
                .locationAddress(act.getLocationAddress())
                .isGatheringEvent(act.getIsGatheringEvent())
                .gatheringEventType(act.getGatheringEventType())
                .scheduleStatus(act.getScheduleStatus())
                .build();
    }

    private static Map<UUID, TourSessionActivityOverride> indexPublished(List<TourSessionActivityOverride> list) {
        Map<UUID, TourSessionActivityOverride> map = new HashMap<>();
        for (TourSessionActivityOverride o : list) {
            if (SessionScheduleMergeHelper.isPublishedForTravelers(o)) {
                map.put(o.getTourActivity().getId(), o);
            } else if (SessionScheduleConstants.PUBLICATION_CANCELLED.equalsIgnoreCase(o.getPublicationStatus())) {
                map.put(o.getTourActivity().getId(), o);
            }
        }
        return map;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.dto.FloraActivityDto;
import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourSession;
import lombok.Builder;
import lombok.Value;

import java.time.*;
import java.util.*;

/**
 * Resolves activity-level schedule from tour itinerary + session dates.
 */
public class FloraJourneyScheduleResolver {

    private FloraJourneyScheduleResolver() {}

    @Value
    @Builder
    public static class ScheduleSnapshot {
        String journeyStatus;
        FloraActivityDto currentActivity;
        FloraActivityDto nextActivity;
        FloraNextMeetingDto nextMeeting;
        List<String> warnings;
        Long freeMinutesUntilMeeting;
        FloraJourneyDtoLegacyDay legacyCurrentDay;
        FloraJourneyDtoLegacyDay legacyNextDay;
    }

    @Value
    @Builder
    public static class FloraJourneyDtoLegacyDay {
        Integer dayNumber;
        String title;
        String summary;
    }

    public static ScheduleSnapshot resolve(
            Booking booking,
            TourSession session,
            List<TourItinerary> itineraryDays,
            ZoneId zone,
            int safetyBufferMinutes) {
        return resolve(booking, session, itineraryDays, zone, safetyBufferMinutes, Instant.now());
    }

    public static ScheduleSnapshot resolve(
            Booking booking,
            TourSession session,
            List<TourItinerary> itineraryDays,
            ZoneId zone,
            int safetyBufferMinutes,
            Instant now) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        List<String> warnings = new ArrayList<>();

        String journeyStatus = resolveJourneyStatus(booking, session, today);
        FloraJourneyDtoLegacyDay legacyCurrent = null;
        FloraJourneyDtoLegacyDay legacyNext = null;

        List<ResolvedSlot> slots = new ArrayList<>();
        if (itineraryDays != null && session != null && session.getStartDate() != null) {
            List<TourItinerary> sortedDays = itineraryDays.stream()
                    .sorted(Comparator.comparing(TourItinerary::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            legacyCurrent = pickLegacyDay(sortedDays, session, today, zone, true);
            legacyNext = pickLegacyDay(sortedDays, session, today, zone, false);

            boolean firstDaySeen = false;
            for (TourItinerary day : sortedDays) {
                int dayNum = day.getDayNumber() != null ? day.getDayNumber() : 1;
                LocalDate activityDate = session.getStartDate().plusDays(Math.max(0, dayNum - 1L));
                if (session.getEndDate() != null && activityDate.isAfter(session.getEndDate())) continue;

                List<TourActivity> activities = day.getActivities() == null ? List.of() :
                        day.getActivities().stream()
                                .sorted(Comparator.comparing(TourActivity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();

                int actIndex = 0;
                for (TourActivity act : activities) {
                    ResolvedSlot slot = toSlot(act, dayNum, activityDate, zone, actIndex == 0 && !firstDaySeen);
                    if (slot != null) slots.add(slot);
                    actIndex++;
                }
                firstDaySeen = true;
            }
        }

        slots.sort(Comparator.comparing(ResolvedSlot::getStartAt, Comparator.nullsLast(Comparator.naturalOrder())));

        FloraActivityDto current = null;
        FloraActivityDto next = null;
        for (ResolvedSlot slot : slots) {
            if (slot.getStartAt() != null && slot.getEndAt() != null
                    && !now.isBefore(slot.getStartAt()) && now.isBefore(slot.getEndAt())) {
                current = toActivityDto(slot);
            }
        }
        for (ResolvedSlot slot : slots) {
            if (slot.getStartAt() != null && slot.getStartAt().isAfter(now)) {
                next = toActivityDto(slot);
                break;
            }
        }

        FloraNextMeetingDto nextMeeting = resolveNextMeeting(slots, booking, session, zone, now, today, warnings);
        Long freeMinutes = computeFreeMinutes(current, nextMeeting, now, safetyBufferMinutes);

        if (slots.isEmpty()) {
            warnings.add("Lịch trình chi tiết theo hoạt động chưa được cập nhật cho tour này.");
        }
        if (nextMeeting == null) {
            warnings.add("Flora chưa có thông tin tập trung chính thức tiếp theo. Bạn hãy theo hướng dẫn của HDV hoặc nhóm chat đoàn nhé.");
        } else if (!FloraScheduleConstants.SCHEDULE_CONFIRMED.equals(nextMeeting.getScheduleStatus())) {
            if (FloraScheduleConstants.SCHEDULE_UNAVAILABLE.equals(nextMeeting.getScheduleStatus())) {
                warnings.add("Điểm hoặc giờ tập trung chính thức chưa được cập nhật.");
            } else {
                warnings.add("Lịch tập trung hiện là dự kiến — Flora sẽ nhắc bạn khi HDV xác nhận giờ và điểm tập trung.");
            }
        }

        return ScheduleSnapshot.builder()
                .journeyStatus(journeyStatus)
                .currentActivity(current)
                .nextActivity(next)
                .nextMeeting(nextMeeting)
                .warnings(warnings.isEmpty() ? List.of() : List.copyOf(warnings))
                .freeMinutesUntilMeeting(freeMinutes)
                .legacyCurrentDay(legacyCurrent)
                .legacyNextDay(legacyNext)
                .build();
    }

    private static FloraNextMeetingDto resolveNextMeeting(
            List<ResolvedSlot> slots,
            Booking booking,
            TourSession session,
            ZoneId zone,
            Instant now,
            LocalDate today,
            List<String> warnings) {

        for (ResolvedSlot slot : slots) {
            if (!slot.isMeetingEvent()) continue;
            if (slot.getStartAt() == null || !slot.getStartAt().isAfter(now)) continue;
            return toMeetingDto(slot, now);
        }

        // Precedence 4: booking pickup for initial departure only (day 1, no activity-based departure)
        if (session != null && session.getStartDate() != null && booking.getPickupAddress() != null
                && !booking.getPickupAddress().isBlank()) {
            if (!today.isBefore(session.getStartDate())) {
                boolean hasDepartureActivity = slots.stream()
                        .anyMatch(s -> s.isMeetingEvent()
                                && FloraScheduleConstants.EVENT_DEPARTURE.equals(s.getEventType()));
                if (!hasDepartureActivity && today.equals(session.getStartDate())) {
                    warnings.add("Điểm đón khởi hành lấy từ thông tin đặt tour — chưa có giờ tập trung xác nhận.");
                    return FloraNextMeetingDto.builder()
                            .eventType(FloraScheduleConstants.EVENT_DEPARTURE)
                            .locationName(booking.getPickupAddress().trim())
                            .location(booking.getPickupAddress().trim())
                            .locationAddress(booking.getPickupAddress().trim())
                            .scheduleStatus(FloraScheduleConstants.SCHEDULE_ESTIMATED)
                            .reminderEligible(false)
                            .build();
                }
            }
        }
        return null;
    }

    private static FloraNextMeetingDto toMeetingDto(ResolvedSlot slot, Instant now) {
        String locationName = slot.getLocationName();
        boolean hasPoint = locationName != null && !locationName.isBlank();
        boolean confirmed = FloraScheduleConstants.SCHEDULE_CONFIRMED.equals(slot.getScheduleStatus());
        boolean reminderEligible = confirmed && hasPoint && slot.getStartAt() != null;

        long minutesUntil = Duration.between(now, slot.getStartAt()).toMinutes();

        return FloraNextMeetingDto.builder()
                .time(confirmed ? slot.getStartAt() : null)
                .location(hasPoint ? locationName : null)
                .locationName(hasPoint ? locationName : null)
                .locationAddress(slot.getLocationAddress())
                .latitude(slot.getLatitude())
                .longitude(slot.getLongitude())
                .minutesUntil(confirmed && slot.getStartAt() != null ? minutesUntil : null)
                .eventType(slot.getEventType())
                .scheduleStatus(slot.getScheduleStatus())
                .reminderEligible(reminderEligible)
                .build();
    }

    private static Long computeFreeMinutes(
            FloraActivityDto current,
            FloraNextMeetingDto nextMeeting,
            Instant now,
            int safetyBufferMinutes) {
        if (current == null || nextMeeting == null || nextMeeting.getTime() == null) return null;
        if (!isFreeExploration(current.getActivityType())) return null;
        if (!FloraScheduleConstants.SCHEDULE_CONFIRMED.equals(nextMeeting.getScheduleStatus())) return null;

        long untilMeeting = Duration.between(now, nextMeeting.getTime()).toMinutes();
        long free = untilMeeting - safetyBufferMinutes;
        return free > 0 ? free : 0L;
    }

    private static boolean isFreeExploration(String activityType) {
        if (activityType == null) return false;
        return "FREE_TIME".equalsIgnoreCase(activityType)
                || "SIGHTSEEING".equalsIgnoreCase(activityType)
                || "SHOPPING".equalsIgnoreCase(activityType);
    }

    private static ResolvedSlot toSlot(
            TourActivity act,
            int dayNumber,
            LocalDate activityDate,
            ZoneId zone,
            boolean firstActivityOfTrip) {

        String scheduleStatus = resolveScheduleStatus(act);
        if (FloraScheduleConstants.SCHEDULE_UNAVAILABLE.equals(scheduleStatus) && act.getStartTime() == null) {
            return null;
        }

        Instant startAt = act.getStartTime() != null
                ? ZonedDateTime.of(activityDate, act.getStartTime(), zone).toInstant()
                : null;
        Instant endAt = null;
        if (act.getEndTime() != null) {
            endAt = ZonedDateTime.of(activityDate, act.getEndTime(), zone).toInstant();
        } else if (startAt != null && act.getDurationMinutes() != null && act.getDurationMinutes() > 0) {
            endAt = startAt.plus(Duration.ofMinutes(act.getDurationMinutes()));
        } else if (startAt != null) {
            endAt = startAt.plus(Duration.ofMinutes(60));
        }

        boolean gathering = Boolean.TRUE.equals(act.getIsGatheringEvent());
        String eventType = gathering
                ? resolveGatheringEventType(act, dayNumber, firstActivityOfTrip)
                : FloraScheduleConstants.EVENT_ACTIVITY;

        return ResolvedSlot.builder()
                .activityId(act.getId())
                .title(act.getTitle())
                .description(act.getDescription())
                .activityType(act.getActivityType())
                .dayNumber(dayNumber)
                .startAt(startAt)
                .endAt(endAt)
                .locationName(act.getLocationName())
                .locationAddress(act.getLocationAddress())
                .latitude(toDouble(act.getLatitude()))
                .longitude(toDouble(act.getLongitude()))
                .scheduleStatus(scheduleStatus)
                .eventType(eventType)
                .meetingEvent(gathering || FloraScheduleConstants.isMeetingEventType(eventType))
                .build();
    }

    private static String resolveGatheringEventType(TourActivity act, int dayNumber, boolean firstActivityOfTrip) {
        if (act.getGatheringEventType() != null && !act.getGatheringEventType().isBlank()) {
            return act.getGatheringEventType().trim().toUpperCase();
        }
        if (dayNumber == 1 && firstActivityOfTrip && "TRANSPORT".equalsIgnoreCase(act.getActivityType())) {
            return FloraScheduleConstants.EVENT_DEPARTURE;
        }
        return FloraScheduleConstants.EVENT_RETURN_TO_BUS;
    }

    static String resolveScheduleStatus(TourActivity act) {
        if (act.getScheduleStatus() != null && !act.getScheduleStatus().isBlank()) {
            return act.getScheduleStatus().trim().toUpperCase();
        }
        if (act.getStartTime() == null) {
            return FloraScheduleConstants.SCHEDULE_UNAVAILABLE;
        }
        return FloraScheduleConstants.SCHEDULE_ESTIMATED;
    }

    private static FloraActivityDto toActivityDto(ResolvedSlot slot) {
        return FloraActivityDto.builder()
                .id(slot.getActivityId())
                .title(slot.getTitle())
                .description(slot.getDescription())
                .startAt(slot.getStartAt())
                .endAt(slot.getEndAt())
                .locationName(slot.getLocationName())
                .locationAddress(slot.getLocationAddress())
                .latitude(slot.getLatitude())
                .longitude(slot.getLongitude())
                .activityType(slot.getActivityType())
                .scheduleStatus(slot.getScheduleStatus())
                .dayNumber(slot.getDayNumber())
                .build();
    }

    private static FloraJourneyDtoLegacyDay pickLegacyDay(
            List<TourItinerary> days,
            TourSession session,
            LocalDate today,
            ZoneId zone,
            boolean current) {
        if (days.isEmpty() || session.getStartDate() == null) return null;
        int dayIndex = (int) Math.max(1, Math.min(days.size(),
                Duration.between(session.getStartDate().atStartOfDay(), today.atStartOfDay()).toDays() + 1));
        TourItinerary currentDay = days.stream()
                .filter(d -> d.getDayNumber() != null && d.getDayNumber() == dayIndex)
                .findFirst()
                .orElse(days.get(0));
        if (current) {
            return FloraJourneyDtoLegacyDay.builder()
                    .dayNumber(currentDay.getDayNumber())
                    .title(currentDay.getTitle())
                    .summary(currentDay.getSummary() != null ? currentDay.getSummary() : currentDay.getDescription())
                    .build();
        }
        int idx = -1;
        for (int i = 0; i < days.size(); i++) {
            if (Objects.equals(days.get(i).getDayNumber(), currentDay.getDayNumber())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) idx = 0;
        if (idx + 1 < days.size()) {
            TourItinerary nd = days.get(idx + 1);
            return FloraJourneyDtoLegacyDay.builder()
                    .dayNumber(nd.getDayNumber())
                    .title(nd.getTitle())
                    .summary(nd.getSummary())
                    .build();
        }
        return null;
    }

    static String resolveJourneyStatus(Booking booking, TourSession session, LocalDate today) {
        if (booking == null || session == null || session.getStartDate() == null) {
            return FloraScheduleConstants.JOURNEY_NOT_AVAILABLE;
        }
        if (session.getEndDate() != null && today.isAfter(session.getEndDate())) {
            return FloraScheduleConstants.JOURNEY_COMPLETED;
        }
        if (today.isBefore(session.getStartDate())) {
            return FloraScheduleConstants.JOURNEY_UPCOMING;
        }
        String status = booking.getStatus();
        if (status != null && Set.of("paid", "confirmed").contains(status.toLowerCase())) {
            return FloraScheduleConstants.JOURNEY_ACTIVE;
        }
        return FloraScheduleConstants.JOURNEY_NOT_AVAILABLE;
    }

    private static Double toDouble(java.math.BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    @Value
    @Builder
    private static class ResolvedSlot {
        UUID activityId;
        String title;
        String description;
        String activityType;
        int dayNumber;
        Instant startAt;
        Instant endAt;
        String locationName;
        String locationAddress;
        Double latitude;
        Double longitude;
        String scheduleStatus;
        String eventType;
        boolean meetingEvent;

        boolean isMeetingEvent() {
            return meetingEvent;
        }
    }
}

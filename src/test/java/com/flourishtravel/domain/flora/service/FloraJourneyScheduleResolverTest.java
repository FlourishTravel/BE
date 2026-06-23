package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.SessionScheduleConstants;
import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloraJourneyScheduleResolverTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private Booking booking;
    private TourSession session;
    private Tour tour;

    @BeforeEach
    void setUp() {
        tour = Tour.builder().title("Đà Lạt 3N2D").build();
        tour.setId(UUID.randomUUID());
        session = TourSession.builder()
                .tour(tour)
                .startDate(LocalDate.of(2026, 6, 25))
                .endDate(LocalDate.of(2026, 6, 27))
                .build();
        session.setId(UUID.randomUUID());
        booking = Booking.builder()
                .user(User.builder().build())
                .session(session)
                .status("confirmed")
                .pickupAddress("Khách sạn Mường Thanh")
                .build();
        booking.setId(UUID.randomUUID());
        booking.getUser().setId(UUID.randomUUID());
    }

    @Test
    void currentActivity_selectedFromStartEndTime() {
        TourActivity current = activity("Tham quan Chợ Đà Lạt", LocalTime.of(9, 0), LocalTime.of(10, 30),
                false, null, FloraScheduleConstants.SCHEDULE_CONFIRMED);
        TourActivity gathering = gatheringActivity("Tập trung lên xe", LocalTime.of(10, 40),
                "Bãi xe cổng chính", FloraScheduleConstants.SCHEDULE_CONFIRMED);

        TourItinerary day1 = day(1, List.of(current, gathering));
        Instant now = ZonedDateTime.of(2026, 6, 25, 9, 30, 0, 0, ZONE).toInstant();

        var snapshot = withClock(day1, now);
        assertNotNull(snapshot.getCurrentActivity());
        assertEquals("Tham quan Chợ Đà Lạt", snapshot.getCurrentActivity().getTitle());
        assertEquals(FloraScheduleConstants.SCHEDULE_CONFIRMED, snapshot.getCurrentActivity().getScheduleStatus());
    }

    @Test
    void nextActivity_isNearestFutureActivity() {
        TourActivity current = activity("Tham quan Chợ Đà Lạt", LocalTime.of(9, 0), LocalTime.of(10, 30),
                false, null, FloraScheduleConstants.SCHEDULE_CONFIRMED);
        TourActivity gathering = gatheringActivity("Tập trung lên xe", LocalTime.of(10, 40),
                "Bãi xe cổng chính", FloraScheduleConstants.SCHEDULE_CONFIRMED);

        TourItinerary day1 = day(1, List.of(current, gathering));
        Instant now = ZonedDateTime.of(2026, 6, 25, 9, 30, 0, 0, ZONE).toInstant();

        var snapshot = withClock(day1, now);
        assertNotNull(snapshot.getNextActivity());
        assertEquals("Tập trung lên xe", snapshot.getNextActivity().getTitle());
    }

    @Test
    void confirmedMeeting_producesReminderEligible() {
        TourActivity gathering = gatheringActivity("Tập trung lên xe", LocalTime.of(10, 40),
                "Bãi xe cổng chính", FloraScheduleConstants.SCHEDULE_CONFIRMED);
        gathering.setLatitude(BigDecimal.valueOf(11.9401));
        gathering.setLongitude(BigDecimal.valueOf(108.4580));

        Instant now = ZonedDateTime.of(2026, 6, 25, 10, 22, 0, 0, ZONE).toInstant();
        var snapshot = withClock(day(1, List.of(gathering)), now);

        FloraNextMeetingDto meeting = snapshot.getNextMeeting();
        assertNotNull(meeting);
        assertEquals(FloraScheduleConstants.SCHEDULE_CONFIRMED, meeting.getScheduleStatus());
        assertTrue(meeting.getReminderEligible());
        assertEquals(18L, meeting.getMinutesUntil());
        assertEquals("Bãi xe cổng chính", meeting.getLocationName());
    }

    @Test
    void estimatedMeeting_notReminderEligible() {
        TourActivity gathering = gatheringActivity("Tập trung lên xe", LocalTime.of(10, 40),
                "Bãi xe cổng chính", FloraScheduleConstants.SCHEDULE_ESTIMATED);

        Instant now = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZONE).toInstant();
        var snapshot = withClock(day(1, List.of(gathering)), now);

        assertNotNull(snapshot.getNextMeeting());
        assertFalse(snapshot.getNextMeeting().getReminderEligible());
        assertNull(snapshot.getNextMeeting().getTime());
    }

    @Test
    void missingMeetingPoint_notReminderEligible() {
        TourActivity gathering = gatheringActivity("Tập trung lên xe", LocalTime.of(10, 40),
                null, FloraScheduleConstants.SCHEDULE_CONFIRMED);

        Instant now = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZONE).toInstant();
        var snapshot = withClock(day(1, List.of(gathering)), now);

        assertNotNull(snapshot.getNextMeeting());
        assertFalse(snapshot.getNextMeeting().getReminderEligible());
    }

    @Test
    void legacyTourWithoutActivityTimes_returnsValidJourneyWithWarnings() {
        TourItinerary day1 = TourItinerary.builder()
                .dayNumber(1)
                .title("Ngày 1")
                .summary("Tham quan Đà Lạt")
                .activities(List.of())
                .build();

        Instant now = ZonedDateTime.of(2026, 6, 25, 8, 0, 0, 0, ZONE).toInstant();
        var snapshot = withClock(List.of(day1), now);

        assertNull(snapshot.getCurrentActivity());
        assertNull(snapshot.getNextActivity());
        assertTrue(snapshot.getWarnings().stream().anyMatch(w -> w.contains("chi tiết")));
        assertEquals(FloraScheduleConstants.JOURNEY_ACTIVE, snapshot.getJourneyStatus());
    }

    @Test
    void legacyTour_doesNotInventDefaultGatheringHour() {
        TourItinerary day1 = day(1, List.of());
        Instant now = ZonedDateTime.of(2026, 6, 25, 6, 30, 0, 0, ZONE).toInstant();
        var snapshot = withClock(day1, now);

        assertTrue(snapshot.getNextMeeting() == null
                || !Boolean.TRUE.equals(snapshot.getNextMeeting().getReminderEligible()));
    }

    @Test
    void publishedOverride_changesMeetingTimeAndSource() {
        TourActivity gathering = gatheringActivity("Tập trung lên xe", LocalTime.of(10, 40),
                "Bãi xe cổng chính", FloraScheduleConstants.SCHEDULE_CONFIRMED);
        gathering.setId(UUID.randomUUID());

        TourSessionActivityOverride override = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_PUBLISHED)
                .startTimeOverride(LocalTime.of(10, 20))
                .locationNameOverride("Cổng phụ phía Đông")
                .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                .version(1)
                .build();

        Instant now = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZONE).toInstant();
        var snapshot = withClockAndOverrides(day(1, List.of(gathering)), now,
                Map.of(gathering.getId(), override));

        assertNotNull(snapshot.getNextMeeting());
        assertEquals(20L, snapshot.getNextMeeting().getMinutesUntil());
        assertEquals("Cổng phụ phía Đông", snapshot.getNextMeeting().getLocationName());
        assertEquals(SessionScheduleConstants.SOURCE_SESSION_OVERRIDE, snapshot.getNextMeeting().getScheduleSource());
        assertTrue(snapshot.getNextMeeting().getReminderEligible());
    }

    @Test
    void cancelledOverride_excludedFromNextActivity() {
        TourActivity gathering = gatheringActivity("Tập trung lên xe", LocalTime.of(10, 40),
                "Bãi xe", FloraScheduleConstants.SCHEDULE_CONFIRMED);
        gathering.setId(UUID.randomUUID());
        TourActivity next = activity("Ăn trưa", LocalTime.of(12, 0), LocalTime.of(13, 0),
                false, null, FloraScheduleConstants.SCHEDULE_CONFIRMED);
        next.setId(UUID.randomUUID());

        TourSessionActivityOverride cancelled = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_CANCELLED)
                .build();

        Instant now = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZONE).toInstant();
        var snapshot = withClockAndOverrides(day(1, List.of(gathering, next)), now,
                Map.of(gathering.getId(), cancelled));

        assertNotNull(snapshot.getNextActivity());
        assertEquals("Ăn trưa", snapshot.getNextActivity().getTitle());
    }

    @Test
    void estimatedOverride_notReminderEligible() {
        TourActivity gathering = gatheringActivity("Tập trung", LocalTime.of(10, 40),
                "Bãi xe", FloraScheduleConstants.SCHEDULE_CONFIRMED);
        gathering.setId(UUID.randomUUID());

        TourSessionActivityOverride override = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_PUBLISHED)
                .scheduleStatus(FloraScheduleConstants.SCHEDULE_ESTIMATED)
                .build();

        Instant now = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZONE).toInstant();
        var snapshot = withClockAndOverrides(day(1, List.of(gathering)), now,
                Map.of(gathering.getId(), override));

        assertNotNull(snapshot.getNextMeeting());
        assertFalse(snapshot.getNextMeeting().getReminderEligible());
    }

    @Test
    void timezone_usesAsiaHoChiMinh() {
        TourActivity act = activity("Sáng sớm", LocalTime.of(7, 0), LocalTime.of(8, 0),
                false, null, FloraScheduleConstants.SCHEDULE_CONFIRMED);
        Instant now = ZonedDateTime.of(2026, 6, 25, 7, 30, 0, 0, ZONE).toInstant();

        var snapshot = withClock(day(1, List.of(act)), now);
        assertNotNull(snapshot.getCurrentActivity());
        assertEquals(
                ZonedDateTime.of(2026, 6, 25, 7, 0, 0, 0, ZONE).toInstant(),
                snapshot.getCurrentActivity().getStartAt());
    }

    private FloraJourneyScheduleResolver.ScheduleSnapshot withClock(TourItinerary day, Instant now) {
        return withClock(List.of(day), now);
    }

    private FloraJourneyScheduleResolver.ScheduleSnapshot withClock(List<TourItinerary> days, Instant now) {
        return FloraJourneyScheduleResolver.resolve(booking, session, days, ZONE, 10, now);
    }

    private FloraJourneyScheduleResolver.ScheduleSnapshot withClockAndOverrides(
            List<TourItinerary> days, Instant now, Map<UUID, TourSessionActivityOverride> overrides) {
        return FloraJourneyScheduleResolver.resolve(booking, session, days, ZONE, 10, now, overrides);
    }

    private FloraJourneyScheduleResolver.ScheduleSnapshot withClockAndOverrides(
            TourItinerary day, Instant now, Map<UUID, TourSessionActivityOverride> overrides) {
        return withClockAndOverrides(List.of(day), now, overrides);
    }

    private static TourItinerary day(int dayNumber, List<TourActivity> activities) {
        return TourItinerary.builder()
                .dayNumber(dayNumber)
                .title("Ngày " + dayNumber)
                .activities(activities)
                .build();
    }

    private static TourActivity activity(
            String title, LocalTime start, LocalTime end, boolean gathering, String location, String status) {
        return TourActivity.builder()
                .title(title)
                .startTime(start)
                .endTime(end)
                .locationName(location)
                .isGatheringEvent(gathering)
                .scheduleStatus(status)
                .sortOrder(0)
                .build();
    }

    private static TourActivity gatheringActivity(String title, LocalTime start, String location, String status) {
        TourActivity act = activity(title, start, start.plusMinutes(15), true, location, status);
        act.setIsGatheringEvent(true);
        act.setGatheringEventType(FloraScheduleConstants.EVENT_RETURN_TO_BUS);
        return act;
    }
}

package com.flourishtravel.domain.tour.schedule;

import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.tour.SessionScheduleConstants;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class SessionScheduleMergeHelperTest {

    @Test
    void publishedOverride_takesPrecedenceOverTemplate() {
        TourActivity template = TourActivity.builder()
                .title("Tập trung lên xe")
                .startTime(LocalTime.of(10, 40))
                .locationName("Bãi xe cổng chính")
                .isGatheringEvent(true)
                .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                .build();

        TourSessionActivityOverride override = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_PUBLISHED)
                .startTimeOverride(LocalTime.of(10, 20))
                .locationNameOverride("Cổng phụ phía Đông")
                .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                .version(2)
                .build();

        var effective = SessionScheduleMergeHelper.resolve(template, override);
        assertEquals(LocalTime.of(10, 20), effective.getStartTime());
        assertEquals("Cổng phụ phía Đông", effective.getLocationName());
        assertEquals(SessionScheduleConstants.SOURCE_SESSION_OVERRIDE, effective.getScheduleSource());
        assertTrue(effective.isFromPublishedOverride());
        assertEquals(2, effective.getScheduleVersion());
    }

    @Test
    void draftOverride_notUsedForTravelers() {
        TourActivity template = gatheringAt(LocalTime.of(10, 40), "Bãi xe");
        TourSessionActivityOverride draft = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_DRAFT)
                .startTimeOverride(LocalTime.of(10, 20))
                .build();

        var effective = SessionScheduleMergeHelper.resolve(template, draft);
        assertEquals(LocalTime.of(10, 40), effective.getStartTime());
        assertEquals(SessionScheduleConstants.SOURCE_TOUR_TEMPLATE, effective.getScheduleSource());
        assertFalse(effective.isFromPublishedOverride());
    }

    @Test
    void cancelledOverride_marksCancelled() {
        TourActivity template = gatheringAt(LocalTime.of(10, 40), "Bãi xe");
        TourSessionActivityOverride cancelled = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_CANCELLED)
                .build();

        var effective = SessionScheduleMergeHelper.resolve(template, cancelled);
        assertTrue(effective.isCancelled());
    }

    @Test
    void resolveDraftPreview_mergesPartialFields() {
        TourActivity template = gatheringAt(LocalTime.of(10, 40), "Bãi xe cổng chính");
        TourSessionActivityOverride draft = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_DRAFT)
                .startTimeOverride(LocalTime.of(10, 20))
                .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                .build();

        var preview = SessionScheduleMergeHelper.resolveDraftPreview(template, draft);
        assertEquals(LocalTime.of(10, 20), preview.getStartTime());
        assertEquals("Bãi xe cổng chính", preview.getLocationName());
        assertTrue(preview.isGatheringEvent());
    }

    private static TourActivity gatheringAt(LocalTime time, String location) {
        return TourActivity.builder()
                .title("Tập trung")
                .startTime(time)
                .endTime(time.plusMinutes(10))
                .locationName(location)
                .isGatheringEvent(true)
                .gatheringEventType(FloraScheduleConstants.EVENT_RETURN_TO_BUS)
                .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                .build();
    }
}

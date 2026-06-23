package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FloraPoiScoringServiceTest {

    @Test
    void noConfirmedMeeting_cannotValidateSchedule() {
        FloraJourneyDto journey = FloraJourneyDto.builder()
                .nextMeeting(FloraNextMeetingDto.builder()
                        .scheduleStatus(FloraScheduleConstants.SCHEDULE_ESTIMATED)
                        .time(Instant.parse("2026-06-25T03:00:00Z"))
                        .locationName("Cổng chính")
                        .build())
                .freeMinutesUntilMeeting(30L)
                .build();

        var ctx = FloraPoiScoringService.fromJourney(journey);
        assertFalse(ctx.isCanValidateSchedule());
        assertFalse(FloraPoiScoringService.fitsSchedule(ctx, 10, 20));
    }

    @Test
    void confirmedMeetingWithEnoughTime_fitsSchedule() {
        FloraJourneyDto journey = FloraJourneyDto.builder()
                .nextMeeting(FloraNextMeetingDto.builder()
                        .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                        .time(Instant.parse("2026-06-25T03:40:00Z"))
                        .locationName("Bãi xe")
                        .build())
                .freeMinutesUntilMeeting(45L)
                .build();

        var ctx = FloraPoiScoringService.fromJourney(journey);
        assertTrue(ctx.isCanValidateSchedule());
        assertTrue(FloraPoiScoringService.fitsSchedule(ctx, 12, 20));
    }

    @Test
    void confirmedMeetingInsufficientTime_doesNotFit() {
        FloraJourneyDto journey = FloraJourneyDto.builder()
                .nextMeeting(FloraNextMeetingDto.builder()
                        .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                        .time(Instant.parse("2026-06-25T03:40:00Z"))
                        .locationName("Bãi xe")
                        .build())
                .freeMinutesUntilMeeting(20L)
                .build();

        var ctx = FloraPoiScoringService.fromJourney(journey);
        assertFalse(FloraPoiScoringService.fitsSchedule(ctx, 12, 10));
        assertFalse(FloraPoiScoringService.fitsSchedule(ctx, 12, 20));
    }

    @Test
    void foodConflictInName_excluded() {
        UserTravelPreference pref = UserTravelPreference.builder()
                .foodAllergies("đậu phộng")
                .build();
        assertEquals(FloraRecommendationConstants.FOOD_EXCLUDED,
                FloraPoiScoringService.resolveFoodMatch("Quán đậu phộng rang", "RESTAURANT", pref, true));
    }

    @Test
    void unknownMenu_doesNotClaimAllergySafety() {
        UserTravelPreference pref = UserTravelPreference.builder()
                .foodAllergies("hải sản")
                .build();
        assertEquals(FloraRecommendationConstants.FOOD_UNKNOWN,
                FloraPoiScoringService.resolveFoodMatch("Quán cà phê A", "CAFE", pref, true));
    }

    @Test
    void budgetUnknownWhenNoPriceData() {
        assertEquals(FloraRecommendationConstants.BUDGET_UNKNOWN,
                FloraPoiScoringService.resolveBudgetMatch(UserTravelPreference.builder().budgetLevel("medium").build(), true));
    }
}

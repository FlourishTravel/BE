package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.chatbot.dto.WeatherForecastDto;
import com.flourishtravel.domain.chatbot.service.ChatbotDataService;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloraJourneyService {

    private static final Set<String> ACTIVE = Set.of("paid", "confirmed");

    private final FloraPrivacyService privacyService;
    private final TourRepository tourRepository;
    private final ChatbotDataService chatbotDataService;

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    @Value("${app.flora.journey.safety-buffer-minutes:10}")
    private int safetyBufferMinutes;

    @Transactional(readOnly = true)
    public FloraJourneyDto getJourney(UUID bookingId, UUID userId) {
        Booking booking = privacyService.requireOwnedBooking(bookingId, userId);
        return buildJourney(booking);
    }

    @Transactional(readOnly = true)
    public FloraJourneyDto buildJourney(Booking booking) {
        TourSession session = booking.getSession();
        Tour tour = session != null ? session.getTour() : null;
        ZoneId zone = ZoneId.of(tourTimezone);
        LocalDate today = LocalDate.now(zone);

        List<TourItinerary> itineraryDays = List.of();
        if (tour != null) {
            var tourOpt = tourRepository.findByIdWithItinerariesAndActivities(tour.getId());
            if (tourOpt.isPresent() && tourOpt.get().getItineraries() != null) {
                itineraryDays = tourOpt.get().getItineraries();
            }
        }

        FloraJourneyScheduleResolver.ScheduleSnapshot snapshot = FloraJourneyScheduleResolver.resolve(
                booking, session, itineraryDays, zone, safetyBufferMinutes);

        FloraJourneyDto.FloraScheduleItemDto currentLegacy = null;
        FloraJourneyDto.FloraScheduleItemDto nextLegacy = null;
        if (snapshot.getLegacyCurrentDay() != null) {
            currentLegacy = FloraJourneyDto.FloraScheduleItemDto.builder()
                    .dayNumber(snapshot.getLegacyCurrentDay().getDayNumber())
                    .title(snapshot.getLegacyCurrentDay().getTitle())
                    .summary(snapshot.getLegacyCurrentDay().getSummary())
                    .build();
        }
        if (snapshot.getLegacyNextDay() != null) {
            nextLegacy = FloraJourneyDto.FloraScheduleItemDto.builder()
                    .dayNumber(snapshot.getLegacyNextDay().getDayNumber())
                    .title(snapshot.getLegacyNextDay().getTitle())
                    .summary(snapshot.getLegacyNextDay().getSummary())
                    .build();
        }

        FloraNextMeetingDto nextMeeting = snapshot.getNextMeeting();
        String meetingPoint = resolveLegacyMeetingPoint(nextMeeting, booking);
        Instant nextGatheringAt = nextMeeting != null ? nextMeeting.getTime() : null;
        Long minutesUntilGathering = nextMeeting != null ? nextMeeting.getMinutesUntil() : null;

        String dest = tour != null ? tour.getDestinationCity() : null;
        String weatherSummary = null;
        if (dest != null && !dest.isBlank()) {
            WeatherForecastDto w = chatbotDataService.getWeatherForecast(dest);
            if (w != null && w.getSummary() != null) weatherSummary = w.getSummary();
        }

        List<String> packing = new ArrayList<>();
        packing.add("Giấy tờ tùy thân / voucher tour");
        packing.add("Điện thoại đầy pin, sạc dự phòng");
        if (weatherSummary != null && weatherSummary.toLowerCase().contains("mưa")) {
            packing.add("Áo mưa / ô");
        }

        List<String> notices = new ArrayList<>();
        notices.add("Liên hệ hotline trên voucher nếu cần hỗ trợ khẩn trong chuyến đi.");

        return FloraJourneyDto.builder()
                .bookingId(booking.getId())
                .bookingStatus(booking.getStatus())
                .tourTitle(tour != null ? tour.getTitle() : null)
                .tourSlug(tour != null ? tour.getSlug() : null)
                .tourId(tour != null ? tour.getId() : null)
                .sessionStartDate(session != null ? session.getStartDate() : null)
                .sessionEndDate(session != null ? session.getEndDate() : null)
                .guestCount(booking.getGuestCount())
                .meetingPoint(meetingPoint)
                .nextGatheringAt(nextGatheringAt)
                .minutesUntilGathering(minutesUntilGathering)
                .currentScheduleItem(currentLegacy)
                .nextScheduleItem(nextLegacy)
                .weatherSummary(weatherSummary)
                .packingReminders(packing)
                .importantNotices(notices)
                .nextMeeting(nextMeeting)
                .journeyStatus(snapshot.getJourneyStatus())
                .currentActivity(snapshot.getCurrentActivity())
                .nextActivity(snapshot.getNextActivity())
                .warnings(snapshot.getWarnings())
                .freeMinutesUntilMeeting(snapshot.getFreeMinutesUntilMeeting())
                .build();
    }

    /**
     * @deprecated No longer uses FLORA_GATHERING_HOUR. Returns confirmed next meeting instant only.
     */
    public Instant computeNextGathering(TourSession session, ZoneId zone) {
        return null;
    }

    /**
     * Next confirmed gathering/meeting instant for reminders — never from default hour fallback.
     */
    @Transactional(readOnly = true)
    public FloraNextMeetingDto resolveReminderMeeting(Booking booking) {
        FloraJourneyDto journey = buildJourney(booking);
        FloraNextMeetingDto meeting = journey.getNextMeeting();
        if (meeting == null || !Boolean.TRUE.equals(meeting.getReminderEligible())) {
            return null;
        }
        return meeting;
    }

    public boolean isActiveTrip(Booking booking) {
        if (booking == null || booking.getStatus() == null) return false;
        if (!ACTIVE.contains(booking.getStatus().toLowerCase())) return false;
        TourSession session = booking.getSession();
        if (session == null || session.getStartDate() == null) return false;
        ZoneId zone = ZoneId.of(tourTimezone);
        LocalDate today = LocalDate.now(zone);
        return !today.isBefore(session.getStartDate())
                && (session.getEndDate() == null || !today.isAfter(session.getEndDate()));
    }

    private static String resolveLegacyMeetingPoint(FloraNextMeetingDto nextMeeting, Booking booking) {
        if (nextMeeting != null && nextMeeting.getLocationName() != null && !nextMeeting.getLocationName().isBlank()) {
            return nextMeeting.getLocationName();
        }
        if (nextMeeting != null && nextMeeting.getLocation() != null && !nextMeeting.getLocation().isBlank()) {
            return nextMeeting.getLocation();
        }
        if (booking.getPickupAddress() != null && !booking.getPickupAddress().isBlank()) {
            return booking.getPickupAddress().trim();
        }
        return null;
    }
}

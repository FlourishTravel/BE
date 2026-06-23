package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.chatbot.dto.WeatherForecastDto;
import com.flourishtravel.domain.chatbot.service.ChatbotDataService;
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

    @Value("${app.flora.default-gathering-hour:7}")
    private int defaultGatheringHour;

    @Value("${app.flora.default-gathering-minute:0}")
    private int defaultGatheringMinute;

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    @Transactional(readOnly = true)
    public FloraJourneyDto getJourney(UUID bookingId, UUID userId) {
        Booking booking = privacyService.requireOwnedBooking(bookingId, userId);
        TourSession session = booking.getSession();
        Tour tour = session != null ? session.getTour() : null;

        ZoneId zone = ZoneId.of(tourTimezone);
        LocalDate today = LocalDate.now(zone);
        Instant nextGathering = computeNextGathering(session, zone);
        Long minutesUntil = nextGathering != null
                ? Duration.between(Instant.now(), nextGathering).toMinutes()
                : null;

        FloraJourneyDto.FloraScheduleItemDto current = null;
        FloraJourneyDto.FloraScheduleItemDto next = null;
        if (tour != null) {
            var tourOpt = tourRepository.findByIdWithItinerariesAndActivities(tour.getId());
            if (tourOpt.isPresent()) {
                List<TourItinerary> days = tourOpt.get().getItineraries();
                if (days != null && !days.isEmpty()) {
                    int dayIndex = session != null && session.getStartDate() != null
                            ? (int) Math.max(1, Math.min(days.size(),
                            Duration.between(session.getStartDate().atStartOfDay(), today.atStartOfDay()).toDays() + 1))
                            : 1;
                    TourItinerary currentDay = days.stream()
                            .filter(d -> d.getDayNumber() != null && d.getDayNumber() == dayIndex)
                            .findFirst()
                            .orElse(days.get(0));
                    current = FloraJourneyDto.FloraScheduleItemDto.builder()
                            .dayNumber(currentDay.getDayNumber())
                            .title(currentDay.getTitle())
                            .summary(currentDay.getSummary() != null ? currentDay.getSummary() : currentDay.getDescription())
                            .build();
                    int nextIdx = Math.min(dayIndex, days.size() - 1);
                    if (nextIdx + 1 < days.size()) {
                        TourItinerary nd = days.get(nextIdx + 1);
                        next = FloraJourneyDto.FloraScheduleItemDto.builder()
                                .dayNumber(nd.getDayNumber())
                                .title(nd.getTitle())
                                .summary(nd.getSummary())
                                .build();
                    }
                }
            }
        }

        String dest = tour != null ? tour.getDestinationCity() : null;
        String weatherSummary = null;
        if (dest != null && !dest.isBlank()) {
            WeatherForecastDto w = chatbotDataService.getWeatherForecast(dest);
            if (w != null && w.getSummary() != null) weatherSummary = w.getSummary();
        }

        String meetingPoint = booking.getPickupAddress();
        if (meetingPoint == null || meetingPoint.isBlank()) {
            meetingPoint = "Điểm tập trung theo lịch tour (xem voucher/email xác nhận)";
        }

        List<String> packing = new ArrayList<>();
        packing.add("Giấy tờ tùy thân / voucher tour");
        packing.add("Điện thoại đầy pin, sạc dự phòng");
        if (weatherSummary != null && weatherSummary.toLowerCase().contains("mưa")) {
            packing.add("Áo mưa / ô");
        }

        FloraNextMeetingDto nextMeeting = FloraNextMeetingDto.builder()
                .time(nextGathering)
                .location(meetingPoint)
                .minutesUntil(minutesUntil)
                .build();

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
                .nextGatheringAt(nextGathering)
                .minutesUntilGathering(minutesUntil)
                .currentScheduleItem(current)
                .nextScheduleItem(next)
                .weatherSummary(weatherSummary)
                .packingReminders(packing)
                .importantNotices(List.of("Liên hệ hotline trên voucher nếu cần hỗ trợ khẩn trong chuyến đi."))
                .nextMeeting(nextMeeting)
                .build();
    }

    public Instant computeNextGathering(TourSession session, ZoneId zone) {
        if (session == null || session.getStartDate() == null) return null;
        LocalDate today = LocalDate.now(zone);
        if (today.isBefore(session.getStartDate()) || (session.getEndDate() != null && today.isAfter(session.getEndDate()))) {
            if (today.isBefore(session.getStartDate())) {
                return ZonedDateTime.of(session.getStartDate(), LocalTime.of(defaultGatheringHour, defaultGatheringMinute), zone).toInstant();
            }
            return null;
        }
        ZonedDateTime gatherToday = ZonedDateTime.of(today, LocalTime.of(defaultGatheringHour, defaultGatheringMinute), zone);
        if (gatherToday.toInstant().isAfter(Instant.now())) {
            return gatherToday.toInstant();
        }
        LocalDate tomorrow = today.plusDays(1);
        if (session.getEndDate() == null || !tomorrow.isAfter(session.getEndDate())) {
            return ZonedDateTime.of(tomorrow, LocalTime.of(defaultGatheringHour, defaultGatheringMinute), zone).toInstant();
        }
        return null;
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
}

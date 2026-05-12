package com.flourishtravel.domain.guide.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.booking.entity.SessionCheckin;
import com.flourishtravel.domain.booking.entity.SessionParticipant;
import com.flourishtravel.domain.booking.entity.SessionParticipantActivityAttendance;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.booking.repository.SessionCheckinRepository;
import com.flourishtravel.domain.booking.repository.SessionParticipantActivityAttendanceRepository;
import com.flourishtravel.domain.booking.repository.SessionParticipantRepository;
import com.flourishtravel.domain.booking.service.SessionParticipantSyncService;
import com.flourishtravel.domain.guide.dto.GuideSessionDetailDto;
import com.flourishtravel.domain.guide.dto.GuideSessionGuestsDto;
import com.flourishtravel.domain.guide.dto.GuideSessionMemberDto;
import com.flourishtravel.domain.guide.dto.GuideSessionSummaryDto;
import com.flourishtravel.domain.guide.dto.ParticipantActivityAttendanceResultDto;
import com.flourishtravel.domain.guide.dto.SessionCheckinResultDto;
import com.flourishtravel.domain.guide.dto.SessionParticipantResultDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourActivityRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuideService {

    private final TourSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final SessionCheckinRepository checkinRepository;
    private final SessionParticipantRepository participantRepository;
    private final SessionParticipantActivityAttendanceRepository activityAttendanceRepository;
    private final SessionParticipantSyncService sessionParticipantSyncService;
    private final TourRepository tourRepository;
    private final TourActivityRepository tourActivityRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GuideSessionSummaryDto> getMySessions(UUID guideId, Integer year, Integer month, LocalDate weekStart) {
        userRepository.findById(guideId).orElseThrow(() -> new ResourceNotFoundException("User", guideId));
        LocalDate from;
        LocalDate to;
        if (year != null && month != null) {
            from = LocalDate.of(year, month, 1);
            to = from.plusMonths(1).minusDays(1);
        } else if (weekStart != null) {
            from = weekStart;
            to = weekStart.plusDays(6);
        } else {
            LocalDate today = LocalDate.now();
            from = today.minusMonths(1);
            to = today.plusMonths(3);
        }
        return sessionRepository.findByTourGuide_IdAndStartDateBetweenOrderByStartDateAsc(guideId, from, to)
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuideSessionDetailDto getSessionById(UUID sessionId, UUID guideId) {
        TourSession session = assertGuideOwnsSession(sessionId, guideId);
        return toDetailDto(session);
    }

    @Transactional(readOnly = true)
    public List<GuideSessionMemberDto> getSessionMembers(UUID sessionId, UUID guideId) {
        TourSession session = assertGuideOwnsSession(sessionId, guideId);
        List<Booking> roster = bookingRepository.findBySessionAndRosterStatusesWithGuests(session);
        Map<UUID, User> unique = new LinkedHashMap<>();
        for (Booking b : roster) {
            User u = b.getUser();
            if (u != null) {
                unique.putIfAbsent(u.getId(), u);
            }
        }
        return unique.values().stream().map(this::toSessionMemberDto).toList();
    }

    @Transactional
    public GuideSessionGuestsDto getSessionGuestsBookings(UUID sessionId, UUID guideId) {
        TourSession session = assertGuideOwnsSession(sessionId, guideId);
        Tour tour = session.getTour();
        sessionParticipantSyncService.syncAllPaidForSession(session);

        long totalSlots = participantRepository.countBySession_Id(sessionId);
        long checkedSlots = participantRepository.countCheckedInBySession_Id(sessionId);
        long checkedOutSlots = participantRepository.countCheckedOutBySession_Id(sessionId);

        List<Booking> rosterBookings = bookingRepository.findBySessionAndRosterStatusesWithGuests(session);
        rosterBookings.sort(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<UUID> allParticipantIds = new ArrayList<>();
        for (Booking b : rosterBookings) {
            for (SessionParticipant px : participantRepository.findByBooking_Id(b.getId())) {
                allParticipantIds.add(px.getId());
            }
        }
        Map<UUID, List<GuideSessionGuestsDto.ActivityAttendanceDto>> activityByParticipant =
                loadActivityAttendanceByParticipant(allParticipantIds);

        Tour tourWithPlan = tour != null
                ? tourRepository.findByIdWithItinerariesAndActivities(tour.getId()).orElse(tour)
                : null;
        List<GuideSessionGuestsDto.ItineraryStopDto> itineraryStops = buildItineraryStops(sessionId, tourWithPlan);

        int withNotes = 0;
        List<GuideSessionGuestsDto.GuideGuestBookingRowDto> rows = new ArrayList<>();

        for (Booking b : rosterBookings) {
            User u = b.getUser();
            int slots = b.getGuestCount() != null && b.getGuestCount() > 0 ? b.getGuestCount() : 1;

            String sr = b.getSpecialRequests();
            if (sr != null && !sr.isBlank()) {
                withNotes++;
            }

            String phoneEffective = (b.getContactPhone() != null && !b.getContactPhone().isBlank())
                    ? b.getContactPhone()
                    : (u != null ? u.getPhone() : null);

            List<GuideSessionGuestsDto.CompanionLineDto> companions = b.getBookingGuests() == null
                    ? List.of()
                    : b.getBookingGuests().stream()
                    .sorted(Comparator.comparing(BookingGuest::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .filter(g -> !SessionParticipantSyncService.isBookingGuestDuplicateOfLead(g, u))
                    .map(g -> GuideSessionGuestsDto.CompanionLineDto.builder()
                            .fullName(g.getFullName())
                            .dateOfBirth(g.getDateOfBirth())
                            .maskedIdNumber(g.getMaskedIdNumber())
                            .build())
                    .toList();

            List<SessionParticipant> parts = participantRepository.findByBooking_Id(b.getId());
            parts.sort(Comparator.comparing(SessionParticipant::getLineIndex, Comparator.nullsLast(Comparator.naturalOrder())));

            List<GuideSessionGuestsDto.ParticipantAttendanceDto> attendance = parts.stream()
                    .map(p -> GuideSessionGuestsDto.ParticipantAttendanceDto.builder()
                            .participantId(p.getId())
                            .rosterKey(p.getRosterKey())
                            .participantRole(p.getParticipantRole())
                            .displayName(p.getDisplayName())
                            .phoneSnapshot(p.getPhoneSnapshot())
                            .lineIndex(p.getLineIndex() != null ? p.getLineIndex() : 0)
                            .checkInAt(p.getCheckInAt())
                            .checkOutAt(p.getCheckOutAt())
                            .activityAttendance(activityByParticipant.getOrDefault(p.getId(), List.of()))
                            .build())
                    .toList();

            boolean allParticipantsCheckedIn = !parts.isEmpty()
                    && parts.stream().allMatch(p -> p.getCheckInAt() != null);

            boolean leadChecked = parts.stream()
                    .filter(p -> SessionParticipantSyncService.ROLE_LEAD.equals(p.getParticipantRole()))
                    .findFirst()
                    .map(p -> p.getCheckInAt() != null)
                    .orElse(false);
            if (!leadChecked && u != null) {
                leadChecked = checkinRepository.existsBySessionAndUserAndCheckInType(session, u, "gathering");
            }

            rows.add(GuideSessionGuestsDto.GuideGuestBookingRowDto.builder()
                    .bookingId(b.getId())
                    .travelerUserId(u != null ? u.getId() : null)
                    .travelerName(u != null ? u.getFullName() : null)
                    .email(u != null ? u.getEmail() : null)
                    .phone(u != null ? u.getPhone() : null)
                    .avatarUrl(u != null ? u.getAvatarUrl() : null)
                    .guestCount(slots)
                    .specialRequests(sr)
                    .effectiveContactPhone(phoneEffective)
                    .pickupAddress(b.getPickupAddress())
                    .emergencyContactName(b.getEmergencyContactName())
                    .emergencyContactPhone(b.getEmergencyContactPhone())
                    .checkedInGathering(leadChecked)
                    .allParticipantsCheckedIn(allParticipantsCheckedIn)
                    .participantAttendance(attendance)
                    .companions(companions)
                    .build());
        }

        return GuideSessionGuestsDto.builder()
                .sessionId(session.getId())
                .tourTitle(tour != null ? tour.getTitle() : null)
                .tourCode(buildTourCode(tour))
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .totalGuestSlots((int) Math.min(totalSlots, Integer.MAX_VALUE))
                .checkedInGuestSlots((int) Math.min(checkedSlots, Integer.MAX_VALUE))
                .checkedOutParticipants((int) Math.min(checkedOutSlots, Integer.MAX_VALUE))
                .paidBookingCount(rosterBookings.size())
                .bookingsWithSpecialRequests(withNotes)
                .itineraryStops(itineraryStops)
                .bookings(rows)
                .build();
    }

    private Map<UUID, List<GuideSessionGuestsDto.ActivityAttendanceDto>> loadActivityAttendanceByParticipant(
            List<UUID> participantIds) {
        if (participantIds.isEmpty()) {
            return Map.of();
        }
        List<SessionParticipantActivityAttendance> rows =
                activityAttendanceRepository.findBySessionParticipant_IdIn(participantIds);
        Map<UUID, List<GuideSessionGuestsDto.ActivityAttendanceDto>> map = new HashMap<>();
        for (SessionParticipantActivityAttendance row : rows) {
            UUID pid = row.getSessionParticipant().getId();
            GuideSessionGuestsDto.ActivityAttendanceDto dto = GuideSessionGuestsDto.ActivityAttendanceDto.builder()
                    .activityId(row.getTourActivity().getId())
                    .checkInAt(row.getCheckInAt())
                    .checkOutAt(row.getCheckOutAt())
                    .build();
            map.computeIfAbsent(pid, k -> new ArrayList<>()).add(dto);
        }
        return map;
    }

    private List<GuideSessionGuestsDto.ItineraryStopDto> buildItineraryStops(UUID sessionId, Tour tour) {
        if (tour == null || tour.getItineraries() == null || tour.getItineraries().isEmpty()) {
            return List.of();
        }
        List<GuideSessionGuestsDto.ItineraryStopDto> out = new ArrayList<>();
        List<TourItinerary> days = tour.getItineraries().stream()
                .sorted(Comparator.comparing(TourItinerary::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (TourItinerary day : days) {
            if (day.getActivities() == null || day.getActivities().isEmpty()) {
                continue;
            }
            List<TourActivity> acts = day.getActivities().stream()
                    .sorted(Comparator.comparing(TourActivity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            for (TourActivity a : acts) {
                long checked = activityAttendanceRepository
                        .countBySessionParticipant_Session_IdAndTourActivity_IdAndCheckInAtIsNotNull(sessionId, a.getId());
                out.add(GuideSessionGuestsDto.ItineraryStopDto.builder()
                        .activityId(a.getId())
                        .dayNumber(day.getDayNumber())
                        .dayTitle(day.getTitle())
                        .sortOrder(a.getSortOrder())
                        .title(a.getTitle())
                        .locationName(a.getLocationName())
                        .activityType(a.getActivityType())
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .checkedInAtStopCount((int) Math.min(checked, Integer.MAX_VALUE))
                        .build());
            }
        }
        return out;
    }

    @Transactional
    public SessionParticipantResultDto checkInParticipant(UUID guideId, UUID sessionId, UUID participantId) {
        assertGuideOwnsSession(sessionId, guideId);
        SessionParticipant p = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionParticipant", participantId));
        if (!p.getSession().getId().equals(sessionId)) {
            throw new BadRequestException("Người tham gia không thuộc lịch này");
        }
        if (p.getCheckInAt() != null) {
            throw new BadRequestException("Đã điểm danh người này rồi");
        }
        p.setCheckInAt(Instant.now());
        SessionParticipant saved = participantRepository.save(p);

        if (SessionParticipantSyncService.ROLE_LEAD.equals(saved.getParticipantRole()) && saved.getUser() != null) {
            TourSession session = sessionRepository.findById(sessionId).orElseThrow();
            User traveler = saved.getUser();
            String type = "gathering";
            if (!checkinRepository.existsBySessionAndUserAndCheckInType(session, traveler, type)) {
                checkinRepository.save(SessionCheckin.builder()
                        .session(session)
                        .user(traveler)
                        .checkInType(type)
                        .build());
            }
        }
        return toParticipantResult(saved);
    }

    @Transactional
    public SessionParticipantResultDto checkOutParticipant(UUID guideId, UUID sessionId, UUID participantId) {
        assertGuideOwnsSession(sessionId, guideId);
        SessionParticipant p = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionParticipant", participantId));
        if (!p.getSession().getId().equals(sessionId)) {
            throw new BadRequestException("Người tham gia không thuộc lịch này");
        }
        if (p.getCheckInAt() == null) {
            throw new BadRequestException("Chưa điểm danh, không thể check-out");
        }
        if (p.getCheckOutAt() != null) {
            throw new BadRequestException("Đã check-out rồi");
        }
        p.setCheckOutAt(Instant.now());
        SessionParticipant saved = participantRepository.save(p);
        return toParticipantResult(saved);
    }

    @Transactional
    public ParticipantActivityAttendanceResultDto checkInParticipantAtActivity(
            UUID guideId, UUID sessionId, UUID participantId, UUID activityId) {
        assertGuideOwnsSession(sessionId, guideId);
        SessionParticipant p = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionParticipant", participantId));
        if (!p.getSession().getId().equals(sessionId)) {
            throw new BadRequestException("Người tham gia không thuộc lịch này");
        }
        TourSession sess = p.getSession();
        Tour tour = sess.getTour();
        if (tour == null || !tourActivityRepository.existsForTour(activityId, tour.getId())) {
            throw new BadRequestException("Hoạt động không thuộc tour của chuyến này");
        }
        TourActivity activity = tourActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("TourActivity", activityId));

        SessionParticipantActivityAttendance row = activityAttendanceRepository
                .findBySessionParticipant_IdAndTourActivity_Id(participantId, activityId)
                .orElseGet(() -> SessionParticipantActivityAttendance.builder()
                        .sessionParticipant(p)
                        .tourActivity(activity)
                        .build());
        if (row.getCheckInAt() != null) {
            throw new BadRequestException("Đã điểm danh tại địa điểm này");
        }
        row.setCheckInAt(Instant.now());
        SessionParticipantActivityAttendance saved = activityAttendanceRepository.save(row);
        return toActivityAttendanceResult(saved);
    }

    @Transactional
    public ParticipantActivityAttendanceResultDto checkOutParticipantAtActivity(
            UUID guideId, UUID sessionId, UUID participantId, UUID activityId) {
        assertGuideOwnsSession(sessionId, guideId);
        SessionParticipant p = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionParticipant", participantId));
        if (!p.getSession().getId().equals(sessionId)) {
            throw new BadRequestException("Người tham gia không thuộc lịch này");
        }
        Tour tour = p.getSession().getTour();
        if (tour == null || !tourActivityRepository.existsForTour(activityId, tour.getId())) {
            throw new BadRequestException("Hoạt động không thuộc tour của chuyến này");
        }
        SessionParticipantActivityAttendance row = activityAttendanceRepository
                .findBySessionParticipant_IdAndTourActivity_Id(participantId, activityId)
                .orElseThrow(() -> new BadRequestException("Chưa điểm danh tại địa điểm này"));
        if (row.getCheckInAt() == null) {
            throw new BadRequestException("Chưa điểm danh, không thể check-out");
        }
        if (row.getCheckOutAt() != null) {
            throw new BadRequestException("Đã check-out tại địa điểm này");
        }
        row.setCheckOutAt(Instant.now());
        SessionParticipantActivityAttendance saved = activityAttendanceRepository.save(row);
        return toActivityAttendanceResult(saved);
    }

    private ParticipantActivityAttendanceResultDto toActivityAttendanceResult(SessionParticipantActivityAttendance saved) {
        return ParticipantActivityAttendanceResultDto.builder()
                .id(saved.getId())
                .sessionParticipantId(saved.getSessionParticipant().getId())
                .tourActivityId(saved.getTourActivity().getId())
                .checkInAt(saved.getCheckInAt())
                .checkOutAt(saved.getCheckOutAt())
                .build();
    }

    @Transactional
    public SessionCheckinResultDto checkin(UUID guideId, UUID sessionId, UUID userId, String checkInType) {
        TourSession session = assertGuideOwnsSession(sessionId, guideId);
        User traveler = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        String type = checkInType != null && !checkInType.isBlank() ? checkInType : "gathering";
        if (checkinRepository.existsBySessionAndUserAndCheckInType(session, traveler, type)) {
            throw new BadRequestException("Đã check-in loại này rồi");
        }
        SessionCheckin checkin = SessionCheckin.builder()
                .session(session)
                .user(traveler)
                .checkInType(type)
                .build();
        SessionCheckin saved = checkinRepository.save(checkin);

        List<Booking> roster = bookingRepository.findBySessionAndRosterStatusesWithGuests(session);
        Booking booking = roster.stream()
                .filter(b -> b.getUser() != null && b.getUser().getId().equals(traveler.getId()))
                .findFirst()
                .orElse(null);
        if (booking != null) {
            sessionParticipantSyncService.syncPaidBooking(booking.getId());
            participantRepository
                    .findBySession_IdAndBooking_IdAndRosterKey(session.getId(), booking.getId(), SessionParticipantSyncService.ROSTER_LEAD)
                    .ifPresent(row -> {
                        if (row.getCheckInAt() == null) {
                            row.setCheckInAt(Instant.now());
                            participantRepository.save(row);
                        }
                    });
        }
        return toCheckinResult(saved);
    }

    private GuideSessionMemberDto toSessionMemberDto(User u) {
        return GuideSessionMemberDto.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }

    private SessionParticipantResultDto toParticipantResult(SessionParticipant p) {
        return SessionParticipantResultDto.builder()
                .participantId(p.getId())
                .sessionId(p.getSession().getId())
                .bookingId(p.getBooking().getId())
                .rosterKey(p.getRosterKey())
                .lineIndex(p.getLineIndex() != null ? p.getLineIndex() : 0)
                .displayName(p.getDisplayName())
                .phoneSnapshot(p.getPhoneSnapshot())
                .participantRole(p.getParticipantRole())
                .checkInAt(p.getCheckInAt())
                .checkOutAt(p.getCheckOutAt())
                .build();
    }

    private SessionCheckinResultDto toCheckinResult(SessionCheckin c) {
        return SessionCheckinResultDto.builder()
                .id(c.getId())
                .sessionId(c.getSession().getId())
                .userId(c.getUser().getId())
                .checkInType(c.getCheckInType())
                .build();
    }

    private GuideSessionSummaryDto toSummaryDto(TourSession session) {
        Tour tour = session.getTour();
        long checkedIn = checkinRepository.countDistinctBySession(session);
        return GuideSessionSummaryDto.builder()
                .sessionId(session.getId())
                .tourId(tour != null ? tour.getId() : null)
                .tourTitle(tour != null ? tour.getTitle() : null)
                .tourCode(buildTourCode(tour))
                .thumbnailUrl(firstImage(tour))
                .location(defaultLocation(tour))
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .status(normalizeStatus(session.getStatus(), session.getStartDate(), session.getEndDate()))
                .currentParticipants(session.getCurrentParticipants() == null ? 0 : session.getCurrentParticipants())
                .maxParticipants(session.getMaxParticipants() == null ? 0 : session.getMaxParticipants())
                .checkedInParticipants((int) checkedIn)
                .build();
    }

    private GuideSessionDetailDto toDetailDto(TourSession session) {
        Tour tour = session.getTour();
        long checkedIn = checkinRepository.countDistinctBySession(session);
        List<GuideSessionDetailDto.ItineraryDayDto> itineraryDays = tour != null && tour.getItineraries() != null
                ? tour.getItineraries().stream()
                .sorted(Comparator.comparing(TourItinerary::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(it -> GuideSessionDetailDto.ItineraryDayDto.builder()
                        .dayNumber(it.getDayNumber())
                        .title(it.getTitle())
                        .summary(it.getSummary())
                        .description(it.getDescription())
                        .activities(mapActivities(it))
                        .build())
                .toList()
                : List.of();

        return GuideSessionDetailDto.builder()
                .sessionId(session.getId())
                .tourId(tour != null ? tour.getId() : null)
                .tourTitle(tour != null ? tour.getTitle() : null)
                .tourCode(buildTourCode(tour))
                .tourDescription(tour != null ? tour.getDescription() : null)
                .thumbnailUrl(firstImage(tour))
                .location(defaultLocation(tour))
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .status(normalizeStatus(session.getStatus(), session.getStartDate(), session.getEndDate()))
                .currentParticipants(session.getCurrentParticipants() == null ? 0 : session.getCurrentParticipants())
                .maxParticipants(session.getMaxParticipants() == null ? 0 : session.getMaxParticipants())
                .checkedInParticipants((int) checkedIn)
                .itineraryDays(itineraryDays)
                .build();
    }

    private List<GuideSessionDetailDto.ItineraryActivityDto> mapActivities(TourItinerary day) {
        if (day.getActivities() == null || day.getActivities().isEmpty()) {
            return List.of();
        }
        return day.getActivities().stream()
                .sorted(Comparator.comparing(TourActivity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> GuideSessionDetailDto.ItineraryActivityDto.builder()
                        .sortOrder(a.getSortOrder())
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .durationMinutes(a.getDurationMinutes())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .activityType(a.getActivityType())
                        .locationName(a.getLocationName())
                        .imageUrl(a.getImageUrl())
                        .build())
                .toList();
    }

    private String defaultLocation(Tour tour) {
        if (tour == null || tour.getLocations() == null || tour.getLocations().isEmpty()) {
            return "Đang cập nhật";
        }
        return tour.getLocations().stream()
                .sorted(Comparator.comparing(l -> l.getVisitOrder() == null ? Integer.MAX_VALUE : l.getVisitOrder()))
                .map(l -> l.getLocationName() == null ? "" : l.getLocationName())
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("Đang cập nhật");
    }

    private String firstImage(Tour tour) {
        if (tour == null || tour.getImages() == null || tour.getImages().isEmpty()) {
            return null;
        }
        return tour.getImages().stream()
                .sorted(Comparator.comparing(TourImage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(TourImage::getImageUrl)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String buildTourCode(Tour tour) {
        if (tour == null || tour.getSlug() == null || tour.getSlug().isBlank()) return "";
        String[] chunks = tour.getSlug().split("-");
        String code = java.util.Arrays.stream(chunks)
                .filter(c -> !c.isBlank())
                .map(c -> String.valueOf(c.charAt(0)))
                .collect(Collectors.joining());
        if (code.length() > 6) {
            code = code.substring(0, 6);
        }
        return code.toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status, LocalDate startDate, LocalDate endDate) {
        String s = status == null ? "scheduled" : status.toLowerCase(Locale.ROOT);
        if ("cancelled".equals(s) || "completed".equals(s)) return s;
        LocalDate today = LocalDate.now();
        LocalDate end = endDate != null ? endDate : startDate;
        if (startDate != null && today.isBefore(startDate)) return "upcoming";
        if (startDate != null && end != null && (!today.isBefore(startDate) && !today.isAfter(end))) return "ongoing";
        if (end != null && today.isAfter(end)) return "completed";
        return "upcoming";
    }

    private TourSession assertGuideOwnsSession(UUID sessionId, UUID guideId) {
        TourSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (session.getTourGuide() == null || !session.getTourGuide().getId().equals(guideId)) {
            throw new BadRequestException("Bạn không phải hướng dẫn viên của lịch này");
        }
        return session;
    }
}

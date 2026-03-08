package com.flourishtravel.domain.guide.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.SessionCheckin;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.booking.repository.SessionCheckinRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuideService {

    private final TourSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final SessionCheckinRepository checkinRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TourSession> getMySessions(UUID guideId, Integer year, Integer month, LocalDate weekStart) {
        User guide = userRepository.findById(guideId).orElseThrow(() -> new ResourceNotFoundException("User", guideId));
        LocalDate from;
        LocalDate to;
        if (year != null && month != null) {
            from = LocalDate.of(year, month, 1);
            to = from.plusMonths(1).minusDays(1);
        } else if (weekStart != null) {
            from = weekStart;
            to = weekStart.plusDays(6);
        } else {
            from = LocalDate.now();
            to = from.plusMonths(3);
        }
        return sessionRepository.findByTourGuide_IdAndStartDateBetweenOrderByStartDateAsc(guideId, from, to);
    }

    @Transactional(readOnly = true)
    public TourSession getSessionById(UUID sessionId, UUID guideId) {
        TourSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (session.getTourGuide() == null || !session.getTourGuide().getId().equals(guideId)) {
            throw new BadRequestException("Bạn không phải hướng dẫn viên của lịch này");
        }
        return session;
    }

    @Transactional(readOnly = true)
    public List<User> getSessionMembers(UUID sessionId, UUID guideId) {
        TourSession session = getSessionById(sessionId, guideId);
        List<Booking> paid = bookingRepository.findBySessionAndStatus(session, "paid");
        return paid.stream().map(Booking::getUser).distinct().collect(Collectors.toList());
    }

    @Transactional
    public SessionCheckin checkin(UUID guideId, UUID sessionId, UUID userId, String checkInType) {
        TourSession session = getSessionById(sessionId, guideId);
        User traveler = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (checkinRepository.existsBySessionAndUserAndCheckInType(session, traveler, checkInType)) {
            throw new BadRequestException("Đã check-in loại này rồi");
        }
        SessionCheckin checkin = SessionCheckin.builder()
                .session(session)
                .user(traveler)
                .checkInType(checkInType != null && !checkInType.isBlank() ? checkInType : "gathering")
                .build();
        return checkinRepository.save(checkin);
    }
}

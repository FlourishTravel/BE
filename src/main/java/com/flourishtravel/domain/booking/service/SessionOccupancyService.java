package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Số chỗ đã giữ trên đợt = tổng {@code guestCount} các đơn chưa hủy.
 * Không dùng bộ đếm cộng/trừ trên session vì dễ lệch với danh sách booking.
 */
@Service
@RequiredArgsConstructor
public class SessionOccupancyService {

    private final BookingRepository bookingRepository;
    private final TourSessionRepository tourSessionRepository;

    public int heldSeats(UUID sessionId) {
        if (sessionId == null) {
            return 0;
        }
        Long sum = bookingRepository.sumHeldGuestCount(sessionId);
        return sum == null ? 0 : Math.max(0, sum.intValue());
    }

    public Map<UUID, Integer> heldSeatsBySessionIds(Collection<UUID> sessionIds) {
        Map<UUID, Integer> map = new HashMap<>();
        if (sessionIds == null || sessionIds.isEmpty()) {
            return map;
        }
        List<Object[]> rows = bookingRepository.sumHeldGuestCountBySessionIds(sessionIds);
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            UUID id = (UUID) row[0];
            int n = row[1] instanceof Number num ? Math.max(0, num.intValue()) : 0;
            map.put(id, n);
        }
        return map;
    }

    @Transactional
    public int sync(TourSession session) {
        if (session == null || session.getId() == null) {
            return 0;
        }
        int held = heldSeats(session.getId());
        session.setCurrentParticipants(held);
        tourSessionRepository.save(session);
        return held;
    }

    /** Ghi lại currentParticipants từ tổng đơn chưa hủy — sửa số chỗ đã lệch. */
    @Transactional
    public int syncAll() {
        List<TourSession> sessions = tourSessionRepository.findAll();
        if (sessions.isEmpty()) {
            return 0;
        }
        Map<UUID, Integer> held = heldSeatsBySessionIds(
                sessions.stream().map(TourSession::getId).filter(id -> id != null).toList());
        int updated = 0;
        for (TourSession session : sessions) {
            int n = session.getId() == null ? 0 : held.getOrDefault(session.getId(), 0);
            if (session.getCurrentParticipants() == null || session.getCurrentParticipants() != n) {
                session.setCurrentParticipants(n);
                tourSessionRepository.save(session);
                updated++;
            }
        }
        return updated;
    }
}

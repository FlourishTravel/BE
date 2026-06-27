package com.flourishtravel.domain.tour.repository;

import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TourSessionRepository extends JpaRepository<TourSession, UUID> {

    List<TourSession> findByTourIdOrderByStartDateAsc(UUID tourId);

    List<TourSession> findByTourGuideAndStartDateBetweenOrderByStartDateAsc(User guide, LocalDate from, LocalDate to);

    List<TourSession> findByTourGuide_IdAndStartDateBetweenOrderByStartDateAsc(UUID guideId, LocalDate from, LocalDate to);

    long countByTourGuide_Id(UUID guideId);

    /** Số session HDV còn phía trước (từ ngày from trở đi, trạng thái scheduled). */
    @Query("""
        SELECT COUNT(s) FROM TourSession s
        WHERE s.tourGuide.id = :guideId
          AND s.startDate >= :from
          AND s.status = 'scheduled'
        """)
    long countUpcomingForGuide(@Param("guideId") UUID guideId, @Param("from") LocalDate from);

    @Query("""
        SELECT COUNT(s) FROM TourSession s
        WHERE s.tourGuide.id = :guideId
          AND s.startDate BETWEEN :from AND :to
          AND s.status = 'scheduled'
        """)
    long countScheduledForGuideBetween(@Param("guideId") UUID guideId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    /**
     * Lấy session khởi hành trong khoảng [from, to] (theo start_date), kèm tour + guide
     * để FE Tour Operations hiển thị calendar/list không bị N+1.
     */
    @Query("""
        SELECT s FROM TourSession s
        LEFT JOIN FETCH s.tour t
        LEFT JOIN FETCH s.tourGuide g
        WHERE s.startDate BETWEEN :from AND :to
        ORDER BY s.startDate ASC
        """)
    List<TourSession> findOperationsBetween(@Param("from") LocalDate from,
                                            @Param("to") LocalDate to);

    /** Đếm số session 1 HDV đang phụ trách trong tháng (cho workload balancing). */
    @Query("""
        SELECT COUNT(s) FROM TourSession s
        WHERE s.tourGuide.id = :guideId
          AND s.startDate BETWEEN :from AND :to
          AND s.status = 'scheduled'
        """)
    long countAssignedInRange(@Param("guideId") UUID guideId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    /** Kiểm tra HDV đã có session khác trùng (overlapping) với khoảng [start, end] hay chưa. */
    @Query("""
        SELECT COUNT(s) > 0 FROM TourSession s
        WHERE s.tourGuide.id = :guideId
          AND s.id <> :excludeSessionId
          AND s.status = 'scheduled'
          AND s.startDate <= :end
          AND s.endDate >= :start
        """)
    boolean existsOverlappingForGuide(@Param("guideId") UUID guideId,
                                      @Param("excludeSessionId") UUID excludeSessionId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    @Query("""
            SELECT s FROM TourSession s
            WHERE LOWER(s.status) = 'scheduled'
              AND s.endDate IS NOT NULL
              AND s.endDate <= :lastEndedDate
            """)
    List<TourSession> findScheduledSessionsEndedBefore(@Param("lastEndedDate") LocalDate lastEndedDate);
}

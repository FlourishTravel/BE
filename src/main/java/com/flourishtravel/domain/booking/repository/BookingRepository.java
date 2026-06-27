package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flourishtravel.domain.tour.entity.TourSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Danh sách booking của user kèm session/tour/category.
     * Không đưa {@code payments} và {@code refunds} vào graph — Hibernate cấm fetch đồng thời nhiều bag (MultipleBagFetchException).
     * Hai collection đó lazy-load kèm {@link org.hibernate.annotations.BatchSize} trên entity {@link Booking}.
     */
    @EntityGraph(attributePaths = {
            "user",
            "session",
            "session.tour",
            "session.tour.category"
    })
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findWithSummaryGraphByUserId(@Param("userId") UUID userId);

    List<Booking> findBySessionAndStatus(TourSession session, String status);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.user
            LEFT JOIN FETCH b.bookingGuests
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithGuests(@Param("id") UUID id);

    /** Mọi booking theo status (so khớp không phân biệt hoa thường) — dùng cho seeder / backfill. */
    @Query("SELECT b FROM Booking b WHERE LOWER(b.status) = LOWER(:status)")
    List<Booking> findAllByStatusIgnoreCase(@Param("status") String status);

    /**
     * Booking đã thanh toán của session — kèm user và danh sách khách trong đơn (không N+1).
     */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.user
            LEFT JOIN FETCH b.bookingGuests
            WHERE b.session = :session AND LOWER(b.status) = LOWER(:status)
            """)
    List<Booking> findBySessionAndStatusWithGuests(@Param("session") TourSession session,
                                                   @Param("status") String status);

    /**
     * Booking đã chốt khách trên session (đã thanh toán / đã xác nhận / đã hoàn thành chuyến) —
     * dùng cho HDV danh sách đoàn và đồng bộ session_participants.
     */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.user
            LEFT JOIN FETCH b.bookingGuests
            WHERE b.session = :session
              AND LOWER(b.status) IN ('paid', 'confirmed', 'completed')
            """)
    List<Booking> findBySessionAndRosterStatusesWithGuests(@Param("session") TourSession session);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status = 'paid'")
    BigDecimal sumTotalAmountByStatusPaid();

    /**
     * Tìm kiếm booking cho admin: lọc theo status, khoảng ngày, từ khoá (tên KH / email / tour title / slug).
     * Native SQL + CAST giúp Postgres bind đúng kiểu (TEXT/TIMESTAMPTZ), tránh lỗi lower(bytea).
     */
    @Query(
        value = """
            SELECT DISTINCT b.*
            FROM public.bookings b
            LEFT JOIN public.users u ON u.id = b.user_id
            LEFT JOIN public.tour_sessions s ON s.id = b.session_id
            LEFT JOIN public.tours t ON t.id = s.tour_id
            WHERE (:status IS NULL OR LOWER(CAST(b.status AS TEXT)) = LOWER(CAST(:status AS TEXT)))
              AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR b.created_at >= CAST(:from AS TIMESTAMPTZ))
              AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR b.created_at <= CAST(:to AS TIMESTAMPTZ))
              AND (
                    LOWER(COALESCE(CAST(u.full_name AS TEXT), '')) LIKE :pattern
                 OR LOWER(COALESCE(CAST(u.email AS TEXT), ''))     LIKE :pattern
                 OR LOWER(COALESCE(CAST(u.phone AS TEXT), ''))     LIKE :pattern
                 OR LOWER(COALESCE(CAST(t.title AS TEXT), ''))     LIKE :pattern
                 OR LOWER(COALESCE(CAST(t.slug AS TEXT), ''))      LIKE :pattern
              )
            ORDER BY b.created_at DESC
            """,
        countQuery = """
            SELECT COUNT(DISTINCT b.id)
            FROM public.bookings b
            LEFT JOIN public.users u ON u.id = b.user_id
            LEFT JOIN public.tour_sessions s ON s.id = b.session_id
            LEFT JOIN public.tours t ON t.id = s.tour_id
            WHERE (:status IS NULL OR LOWER(CAST(b.status AS TEXT)) = LOWER(CAST(:status AS TEXT)))
              AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR b.created_at >= CAST(:from AS TIMESTAMPTZ))
              AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR b.created_at <= CAST(:to AS TIMESTAMPTZ))
              AND (
                    LOWER(COALESCE(CAST(u.full_name AS TEXT), '')) LIKE :pattern
                 OR LOWER(COALESCE(CAST(u.email AS TEXT), ''))     LIKE :pattern
                 OR LOWER(COALESCE(CAST(u.phone AS TEXT), ''))     LIKE :pattern
                 OR LOWER(COALESCE(CAST(t.title AS TEXT), ''))     LIKE :pattern
                 OR LOWER(COALESCE(CAST(t.slug AS TEXT), ''))      LIKE :pattern
              )
            """,
        nativeQuery = true)
    Page<Booking> adminSearch(@Param("status") String status,
                              @Param("pattern") String pattern,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);

    /** Tổng số tiền đã thanh toán thành công của các booking có createdAt trong khoảng. */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Booking b JOIN b.payments p
        WHERE p.status = 'paid'
          AND b.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumPaidPaymentsBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Số booking tạo trong khoảng. */
    long countByCreatedAtBetween(Instant from, Instant to);

    /**
     * Tổng "số dư phải thu" (totalAmount - tổng payments paid) của booking chưa hoàn thành.
     * Postgres-safe: tính trong DB để tránh kéo cả bảng.
     */
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount - (
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.booking = b AND p.status = 'paid'
        )), 0)
        FROM Booking b
        WHERE b.status NOT IN ('cancelled', 'completed')
        """)
    BigDecimal sumPendingDeposit();

    /** Số yêu cầu hoàn tiền đang chờ xử lý (status='pending'). */
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.status = 'pending'")
    long countPendingRefunds();

    // ---------- Customer-level aggregates (cho trang Quản Lý Khách Hàng) ----------

    /** Tổng số tiền KH đã thanh toán thành công (sum payment.amount where status=paid). */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Booking b JOIN b.payments p
        WHERE b.user.id = :userId AND p.status = 'paid'
        """)
    BigDecimal sumPaidByUser(@Param("userId") UUID userId);

    /** Đếm booking của KH. */
    long countByUser_Id(UUID userId);

    /** Đếm booking của KH theo status (case-insensitive lưu lower). */
    long countByUser_IdAndStatus(UUID userId, String status);

    /** Lấy max createdAt của booking thuộc KH (phục vụ "last active"). */
    @Query("SELECT MAX(b.createdAt) FROM Booking b WHERE b.user.id = :userId")
    Instant findLastBookingAtByUser(@Param("userId") UUID userId);

    /** Tổng refund pending amount của KH. */
    @Query("""
        SELECT COALESCE(SUM(r.amount), 0)
        FROM Booking b JOIN b.refunds r
        WHERE b.user.id = :userId AND r.status = 'pending'
        """)
    BigDecimal sumPendingRefundByUser(@Param("userId") UUID userId);

    /** Bookings gần đây nhất của KH (giới hạn pageable.size). */
    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.session s
        LEFT JOIN FETCH s.tour t
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findRecentByUser(@Param("userId") UUID userId, Pageable pageable);

    /** Booking + session + tour + HDV — phục vụ chat theo đơn (một round-trip). */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.user
            LEFT JOIN FETCH b.session s
            LEFT JOIN FETCH s.tour
            LEFT JOIN FETCH s.tourGuide
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithSessionTourForChat(@Param("id") UUID id);

    /**
     * Chi tiết đơn cho khách — không fetch payments/refunds trong cùng graph (tránh MultipleBagFetchException).
     */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.user
            LEFT JOIN FETCH b.promotion
            LEFT JOIN FETCH b.session s
            LEFT JOIN FETCH s.tour t
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH s.tourGuide
            LEFT JOIN FETCH b.bookingGuests
            WHERE b.id = :id AND b.user.id = :userId
            """)
    Optional<Booking> findDetailForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Đơn còn hiệu lực (không hủy / chưa hoàn thành chuyến) có lịch khởi hành trùng ngày với khoảng [rangeStart, rangeEnd].
     * Dùng chặn đặt hai tour chồng lịch khi chuyến trước chưa kết thúc.
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b JOIN b.session s
            WHERE b.user.id = :userId
              AND LOWER(b.status) NOT IN ('cancelled', 'completed')
              AND s.endDate >= :today
              AND s.startDate <= :rangeEnd
              AND s.endDate >= :rangeStart
            """)
    long countActiveBookingsOverlappingDateRange(@Param("userId") UUID userId,
                                                 @Param("today") LocalDate today,
                                                 @Param("rangeStart") LocalDate rangeStart,
                                                 @Param("rangeEnd") LocalDate rangeEnd);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user u
            JOIN FETCH b.session s
            JOIN FETCH s.tour
            WHERE LOWER(b.status) IN :statuses
              AND s.startDate <= :today AND s.endDate >= :today
            """)
    List<Booking> findActiveForFloraReminders(@Param("today") LocalDate today,
                                              @Param("statuses") Set<String> statuses);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user u
            JOIN FETCH b.session s
            WHERE LOWER(b.status) IN :statuses
              AND s.endDate = :yesterday
            """)
    List<Booking> findRecentlyCompletedForFlora(@Param("yesterday") LocalDate yesterday,
                                                @Param("statuses") Set<String> statuses);

    /** Đơn đã qua ngày kết thúc chuyến (endDate &lt; hôm nay) nhưng booking chưa đóng. */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.session s
            WHERE LOWER(b.status) IN :statuses
              AND s.endDate IS NOT NULL
              AND s.endDate <= :lastEndedDate
            """)
    List<Booking> findEndedBookingsWithStatuses(@Param("lastEndedDate") LocalDate lastEndedDate,
                                                @Param("statuses") Set<String> statuses);
}

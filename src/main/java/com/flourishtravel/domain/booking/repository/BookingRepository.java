package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flourishtravel.domain.tour.entity.TourSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    List<Booking> findBySessionAndStatus(TourSession session, String status);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status = 'paid'")
    BigDecimal sumTotalAmountByStatusPaid();

    /**
     * Tìm kiếm booking cho admin: lọc theo status, khoảng ngày, từ khoá (tên KH / email / tour title / slug).
     * LƯU Ý: truyền pattern dạng "%abc%". Nếu không lọc, truyền "%%". Tránh null để Postgres không
     * gặp lỗi lower(bytea) does not exist. Việc lọc theo bookingCode (FT-xxxxxxxx) do service làm.
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN b.user u
        LEFT JOIN b.session s
        LEFT JOIN s.tour t
        WHERE (:status IS NULL OR LOWER(b.status) = LOWER(:status))
          AND (:from IS NULL OR b.createdAt >= :from)
          AND (:to IS NULL OR b.createdAt <= :to)
          AND (
                LOWER(COALESCE(u.fullName, '')) LIKE :pattern
             OR LOWER(COALESCE(u.email, ''))    LIKE :pattern
             OR LOWER(COALESCE(u.phone, ''))    LIKE :pattern
             OR LOWER(COALESCE(t.title, ''))    LIKE :pattern
             OR LOWER(COALESCE(t.slug, ''))     LIKE :pattern
          )
        ORDER BY b.createdAt DESC
        """)
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
}

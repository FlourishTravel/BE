package com.flourishtravel.domain.payment.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByBooking(Booking booking);

    Page<Refund> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Tìm kiếm refund cho admin Tài chính.
     * Tương tự PaymentRepository.adminSearch nhưng cho refund.
     */
    @Query("""
        SELECT DISTINCT r FROM Refund r
        LEFT JOIN r.booking b
        LEFT JOIN b.user u
        LEFT JOIN b.session s
        LEFT JOIN s.tour t
        WHERE (:status IS NULL OR r.status = :status)
          AND r.createdAt >= :from
          AND r.createdAt <= :to
          AND (
                LOWER(COALESCE(u.fullName, '')) LIKE :pattern
             OR LOWER(COALESCE(u.email, ''))    LIKE :pattern
             OR LOWER(COALESCE(t.title, ''))    LIKE :pattern
             OR LOWER(COALESCE(r.reason, ''))   LIKE :pattern
          )
        ORDER BY r.createdAt DESC
        """)
    Page<Refund> adminSearch(@Param("status") String status,
                             @Param("pattern") String pattern,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);

    /** Tổng refund processed trong khoảng (cho monthly). */
    @Query("""
        SELECT COALESCE(SUM(r.amount), 0) FROM Refund r
        WHERE r.status = 'processed'
          AND r.processedAt BETWEEN :from AND :to
        """)
    BigDecimal sumProcessedBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Tổng refund đã xử lý tới nay. */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.status = 'processed'")
    BigDecimal sumAllProcessed();

    /** Tổng refund đang pending. */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.status = 'pending'")
    BigDecimal sumPending();
}

package com.flourishtravel.domain.payment.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByBooking(Booking booking);

    Optional<Payment> findByOrderId(String orderId);

    Page<Payment> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Tìm kiếm payment cho admin Tài chính.
     * @param status    pending|paid|failed|refunded — null = bỏ qua
     * @param provider  momo|vnpay|bank_transfer|manual — null = bỏ qua
     * @param pattern   LIKE pattern "%abc%" (search theo customer/tour/orderId). Truyền "%%" khi không lọc.
     * @param from/to   khoảng createdAt
     */
    @Query("""
        SELECT DISTINCT p FROM Payment p
        LEFT JOIN p.booking b
        LEFT JOIN b.user u
        LEFT JOIN b.session s
        LEFT JOIN s.tour t
        WHERE (:status IS NULL OR p.status = :status)
          AND (:provider IS NULL OR p.provider = :provider)
          AND p.createdAt >= :from
          AND p.createdAt <= :to
          AND (
                LOWER(COALESCE(u.fullName, '')) LIKE :pattern
             OR LOWER(COALESCE(u.email, ''))    LIKE :pattern
             OR LOWER(COALESCE(t.title, ''))    LIKE :pattern
             OR LOWER(COALESCE(p.orderId, ''))  LIKE :pattern
             OR LOWER(COALESCE(p.providerTransId, '')) LIKE :pattern
          )
        ORDER BY p.createdAt DESC
        """)
    Page<Payment> adminSearch(@Param("status") String status,
                              @Param("provider") String provider,
                              @Param("pattern") String pattern,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);

    /** Tổng tiền paid trong khoảng (cho monthly revenue). */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
        WHERE p.status = 'paid'
          AND p.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumPaidBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Tổng fee các payment đã thanh toán thành công. */
    @Query("""
        SELECT COALESCE(SUM(p.feeAmount), 0) FROM Payment p
        WHERE p.status = 'paid'
        """)
    BigDecimal sumTotalFees();

    /** Số giao dịch trong khoảng theo trạng thái (cho success rate). */
    long countByCreatedAtBetween(Instant from, Instant to);
    long countByStatusAndCreatedAtBetween(String status, Instant from, Instant to);

    /** Phân bổ theo provider (tổng amount paid + count). */
    @Query("""
        SELECT p.provider as provider, SUM(p.amount) as total, COUNT(p) as cnt
        FROM Payment p
        WHERE p.status = 'paid'
        GROUP BY p.provider
        ORDER BY SUM(p.amount) DESC
        """)
    List<Object[]> aggregateByProvider();

    /** Top tours theo revenue (paid). */
    @Query("""
        SELECT t.id as id, t.title as title, t.slug as slug,
               SUM(p.amount) as total, COUNT(DISTINCT b.id) as bookings
        FROM Payment p
        JOIN p.booking b
        JOIN b.session s
        JOIN s.tour t
        WHERE p.status = 'paid'
        GROUP BY t.id, t.title, t.slug
        ORDER BY SUM(p.amount) DESC
        """)
    List<Object[]> topToursByRevenue(Pageable pageable);

    /** Sum paid trước 1 thời điểm (cho net revenue tới hiện tại). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'paid'")
    BigDecimal sumAllPaid();
}

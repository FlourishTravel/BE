package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flourishtravel.domain.tour.entity.TourSession;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    List<Booking> findBySessionAndStatus(TourSession session, String status);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status = 'paid'")
    BigDecimal sumTotalAmountByStatusPaid();
}

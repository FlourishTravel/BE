package com.flourishtravel.domain.review.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByBooking(Booking booking);

    boolean existsByBooking(Booking booking);

    boolean existsByBooking_Id(UUID bookingId);
}

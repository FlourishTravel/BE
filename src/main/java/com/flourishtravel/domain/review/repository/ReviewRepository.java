package com.flourishtravel.domain.review.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByBooking(Booking booking);

    boolean existsByBooking(Booking booking);

    boolean existsByBooking_Id(UUID bookingId);

    @Query("""
            SELECT r FROM Review r
            LEFT JOIN FETCH r.user
            LEFT JOIN FETCH r.tour
            ORDER BY r.createdAt DESC
            """)
    List<Review> findAllAdmin();

    @Query("""
            SELECT r FROM Review r
            LEFT JOIN FETCH r.user
            LEFT JOIN FETCH r.tour
            WHERE r.isPublished = true
              AND (:tourId IS NULL OR r.tour.id = :tourId)
            ORDER BY r.createdAt DESC
            """)
    List<Review> findPublic(@Param("tourId") UUID tourId);

    @Query("""
            SELECT r FROM Review r
            LEFT JOIN FETCH r.user
            LEFT JOIN FETCH r.tour
            WHERE r.isPublished = true AND r.isFeatured = true
            ORDER BY r.createdAt DESC
            """)
    List<Review> findFeaturedPublic();
}

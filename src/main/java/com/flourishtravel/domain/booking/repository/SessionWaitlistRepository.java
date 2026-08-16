package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.SessionWaitlist;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionWaitlistRepository extends JpaRepository<SessionWaitlist, UUID> {

    Optional<SessionWaitlist> findByUserAndSession(User user, TourSession session);

    Optional<SessionWaitlist> findByUserAndTourAndSessionIsNull(User user, Tour tour);

    boolean existsByUserAndSession(User user, TourSession session);

    boolean existsByUserAndTourAndSessionIsNull(User user, Tour tour);

    @Query("""
            SELECT w FROM SessionWaitlist w
            JOIN FETCH w.user
            LEFT JOIN FETCH w.tour
            WHERE w.tour.id = :tourId AND w.session IS NULL
            ORDER BY w.createdAt DESC
            """)
    List<SessionWaitlist> findTourLevelByTourId(@Param("tourId") UUID tourId);

    @Query("""
            SELECT w FROM SessionWaitlist w
            JOIN FETCH w.user
            JOIN FETCH w.session s
            JOIN FETCH s.tour
            WHERE s.tour.id = :tourId
            ORDER BY w.createdAt DESC
            """)
    List<SessionWaitlist> findSessionLevelByTourId(@Param("tourId") UUID tourId);
}

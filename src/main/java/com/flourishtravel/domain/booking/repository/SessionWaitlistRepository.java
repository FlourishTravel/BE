package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.SessionWaitlist;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionWaitlistRepository extends JpaRepository<SessionWaitlist, UUID> {

    Optional<SessionWaitlist> findByUserAndSession(User user, TourSession session);

    Optional<SessionWaitlist> findByUserAndTourAndSessionIsNull(User user, Tour tour);

    boolean existsByUserAndSession(User user, TourSession session);

    boolean existsByUserAndTourAndSessionIsNull(User user, Tour tour);
}

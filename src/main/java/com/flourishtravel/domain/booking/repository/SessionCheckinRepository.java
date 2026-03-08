package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.SessionCheckin;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionCheckinRepository extends JpaRepository<SessionCheckin, UUID> {

    List<SessionCheckin> findBySession(TourSession session);

    boolean existsBySessionAndUserAndCheckInType(TourSession session, User user, String checkInType);
}

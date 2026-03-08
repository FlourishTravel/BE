package com.flourishtravel.domain.tour.repository;

import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TourSessionRepository extends JpaRepository<TourSession, UUID> {

    List<TourSession> findByTourIdOrderByStartDateAsc(UUID tourId);

    List<TourSession> findByTourGuideAndStartDateBetweenOrderByStartDateAsc(User guide, LocalDate from, LocalDate to);

    List<TourSession> findByTourGuide_IdAndStartDateBetweenOrderByStartDateAsc(UUID guideId, LocalDate from, LocalDate to);
}

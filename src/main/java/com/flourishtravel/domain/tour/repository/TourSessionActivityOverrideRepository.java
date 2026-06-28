package com.flourishtravel.domain.tour.repository;

import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourSessionActivityOverrideRepository extends JpaRepository<TourSessionActivityOverride, UUID> {

    List<TourSessionActivityOverride> findByTourSession_Id(UUID sessionId);

    Optional<TourSessionActivityOverride> findByTourSession_IdAndTourActivity_Id(UUID sessionId, UUID activityId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM tour_session_activity_overrides o
            USING tour_activities ta
            INNER JOIN tour_itineraries ti ON ti.id = ta.itinerary_id
            WHERE o.tour_activity_id = ta.id
              AND ti.tour_id = :tourId
            """, nativeQuery = true)
    void deleteByTourActivityTourId(@Param("tourId") UUID tourId);
}

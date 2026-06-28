package com.flourishtravel.domain.tour.repository;

import com.flourishtravel.domain.tour.entity.TourActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TourActivityRepository extends JpaRepository<TourActivity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM tour_activities ta
            USING tour_itineraries ti
            WHERE ta.itinerary_id = ti.id
              AND ti.tour_id = :tourId
            """, nativeQuery = true)
    void deleteByTourId(@Param("tourId") UUID tourId);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM TourActivity a
            JOIN a.itinerary it
            WHERE a.id = :activityId AND it.tour.id = :tourId
            """)
    boolean existsForTour(@Param("activityId") UUID activityId, @Param("tourId") UUID tourId);
}

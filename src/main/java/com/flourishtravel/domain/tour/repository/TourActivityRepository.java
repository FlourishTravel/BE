package com.flourishtravel.domain.tour.repository;

import com.flourishtravel.domain.tour.entity.TourActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TourActivityRepository extends JpaRepository<TourActivity, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM TourActivity a
            JOIN a.itinerary it
            WHERE a.id = :activityId AND it.tour.id = :tourId
            """)
    boolean existsForTour(@Param("activityId") UUID activityId, @Param("tourId") UUID tourId);
}

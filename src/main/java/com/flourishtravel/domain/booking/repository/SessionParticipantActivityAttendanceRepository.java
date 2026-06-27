package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.SessionParticipantActivityAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionParticipantActivityAttendanceRepository
        extends JpaRepository<SessionParticipantActivityAttendance, UUID> {

    List<SessionParticipantActivityAttendance> findBySessionParticipant_Id(UUID sessionParticipantId);

    List<SessionParticipantActivityAttendance> findBySessionParticipant_IdIn(Collection<UUID> sessionParticipantIds);

    Optional<SessionParticipantActivityAttendance> findBySessionParticipant_IdAndTourActivity_Id(
            UUID sessionParticipantId, UUID tourActivityId);

    long countBySessionParticipant_Session_IdAndTourActivity_IdAndCheckInAtIsNotNull(
            UUID sessionId, UUID tourActivityId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM SessionParticipantActivityAttendance a WHERE a.tourActivity.itinerary.tour.id = :tourId")
    void deleteByTourActivityTourId(@Param("tourId") UUID tourId);
}

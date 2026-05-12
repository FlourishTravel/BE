package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    Optional<SessionParticipant> findBySession_IdAndBooking_IdAndRosterKey(
            UUID sessionId, UUID bookingId, String rosterKey);

    long countBySession_Id(UUID sessionId);

    @Query("""
            SELECT COUNT(p) FROM SessionParticipant p
            WHERE p.session.id = :sessionId AND p.checkInAt IS NOT NULL
            """)
    long countCheckedInBySession_Id(@Param("sessionId") UUID sessionId);

    @Query("""
            SELECT COUNT(p) FROM SessionParticipant p
            WHERE p.session.id = :sessionId AND p.checkOutAt IS NOT NULL
            """)
    long countCheckedOutBySession_Id(@Param("sessionId") UUID sessionId);

    List<SessionParticipant> findByBooking_Id(UUID bookingId);
}

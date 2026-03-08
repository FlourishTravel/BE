package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingGuestRepository extends JpaRepository<BookingGuest, UUID> {

    List<BookingGuest> findByBookingOrderBySortOrderAsc(Booking booking);
}

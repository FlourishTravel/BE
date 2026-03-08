package com.flourishtravel.domain.payment.repository;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByBooking(Booking booking);

    Optional<Payment> findByOrderId(String orderId);

    Page<Payment> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

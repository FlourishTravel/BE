package com.flourishtravel.domain.admin.service;

import com.flourishtravel.domain.admin.dto.AdminStatsResponse;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalTours = tourRepository.count();
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long paidBookings = bookingRepository.countByStatus("paid");
        BigDecimal revenueTotal = paymentRepository.findAll().stream()
                .filter(p -> {
                    String s = p.getStatus();
                    return "paid".equalsIgnoreCase(s) || "success".equalsIgnoreCase(s);
                })
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return AdminStatsResponse.builder()
                .totalTours(totalTours)
                .totalUsers(totalUsers)
                .totalBookings(totalBookings)
                .paidBookings(paidBookings)
                .revenueTotal(revenueTotal)
                .build();
    }
}

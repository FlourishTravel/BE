package com.flourishtravel.domain.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminStatsResponse {

    private long totalTours;
    private long totalUsers;
    private long totalBookings;
    private long paidBookings;
    private BigDecimal revenueTotal;
}

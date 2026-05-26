package com.flourishtravel.domain.planner.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PlannerGenerateRequest {
    /** Slug điểm đến: bangkok, phuket, ... */
    private List<String> destinations;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer adults;
    private Integer children;
    /** Ngân sách tổng chuyến đi (VND) */
    private Long budgetVnd;
    /** true = budgetVnd là mỗi người */
    private Boolean budgetPerPerson;
    private List<String> styles;
    /** 0 = thư giãn, 100 = khám phá */
    private Integer experienceLevel;
    private Boolean includeFlight;
    /** 3, 4 hoặc 5 */
    private Integer hotelStars;
    private List<String> transport;
}

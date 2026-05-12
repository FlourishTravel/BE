package com.flourishtravel.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffStatsDto {

    private long totalStaff;
    private long activeCount;
    private long onLeaveCount;
    private long inactiveCount;

    private long adminCount;
    private long guideCount;
    private long internalStaffCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptBreakdown {
        private long sales;
        private long operations;
        private long finance;
        private long adminDept;
        private long guides;
        private long other;
    }

    private DeptBreakdown byDepartment;
}

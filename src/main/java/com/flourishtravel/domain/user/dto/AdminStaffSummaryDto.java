package com.flourishtravel.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStaffSummaryDto {

    private UUID id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;

    /** ADMIN | TOUR_GUIDE | STAFF */
    private String roleName;
    /** Nhãn tiếng Việt cho vai hệ thống */
    private String roleLabel;

    private String jobTitle;
    /** SALES | OPERATIONS | FINANCE | ADMIN | GUIDE ... */
    private String department;
    private String departmentLabel;

    /** active | on_leave | inactive */
    private String employmentStatus;
    private boolean active;

    private Instant lastLoginAt;
}

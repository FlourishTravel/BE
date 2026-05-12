package com.flourishtravel.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStaffDetailDto {

    private UUID id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;

    private LocalDate dateOfBirth;
    private String gender;
    private String address;

    private String roleName;
    private String roleLabel;
    private String jobTitle;
    private String department;
    private String departmentLabel;

    private String employmentStatus;
    private boolean active;

    private String adminNote;
    private Instant lastLoginAt;
    private Instant joinedAt;

    /** Chỉ HDV: số tour/session sắp tới (từ hôm nay, scheduled). */
    private Long upcomingSessionsCount;

    /** Chỉ HDV: số session đã gán trong 90 ngày tới (ước lượng workload). */
    private Long scheduledSessionsNext90Days;
}

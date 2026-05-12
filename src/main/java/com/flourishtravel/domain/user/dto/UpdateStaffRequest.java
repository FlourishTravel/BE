package com.flourishtravel.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffRequest {

    @Size(max = 255)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(male|female|other)?$")
    private String gender;

    private String address;

    @Size(max = 120)
    private String jobTitle;

    @Size(max = 40)
    private String department;

    @Pattern(regexp = "^(ADMIN|TOUR_GUIDE|STAFF)?$")
    private String roleName;

    @Pattern(regexp = "^(active|on_leave|inactive)?$")
    private String employmentStatus;

    private String adminNote;
}

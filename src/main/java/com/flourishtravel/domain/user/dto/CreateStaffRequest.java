package com.flourishtravel.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 20)
    private String phone;

    /**
     * ADMIN | TOUR_GUIDE | STAFF — không được TRAVELER.
     */
    @NotBlank
    @Pattern(regexp = "^(ADMIN|TOUR_GUIDE|STAFF)$", message = "role phải là ADMIN, TOUR_GUIDE hoặc STAFF")
    private String roleName;

    @Size(max = 120)
    private String jobTitle;

    /** SALES | OPERATIONS | FINANCE | ADMIN | GUIDE hoặc tùy chỉnh ngắn */
    @Size(max = 40)
    private String department;

    @Pattern(regexp = "^(active|on_leave)?$", message = "employmentStatus phải là active hoặc on_leave (hoặc để trống)")
    private String employmentStatus;
}

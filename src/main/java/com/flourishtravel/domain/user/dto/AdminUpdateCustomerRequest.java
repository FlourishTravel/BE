package com.flourishtravel.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Payload cho admin cập nhật thông tin khách hàng.
 * Tất cả trường tuỳ chọn (PATCH-like).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateCustomerRequest {

    @Size(max = 255)
    private String fullName;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(male|female|other)?$", message = "gender phải là male, female hoặc other")
    private String gender;

    private String address;

    @Size(max = 100)
    private String nationality;

    private String adminNote;

    private Boolean marketingOptIn;
}

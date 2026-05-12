package com.flourishtravel.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tóm tắt 1 khách hàng cho bảng admin Quản Lý Khách Hàng.
 *
 * Các trường suy luận (không lưu DB):
 *  - tier         : VIP | GOLD | SILVER | STANDARD — tính theo totalSpent (sum payments=paid)
 *  - bookingCount : tổng booking của khách
 *  - totalSpent   : tổng số tiền đã thanh toán (sum payments.amount where status=paid)
 *  - lastActiveAt : max(booking.createdAt) hoặc user.lastLoginAt — để xếp hạng hoạt động
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerSummaryDto {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String role;

    private boolean active;
    private boolean marketingOptIn;

    private long bookingCount;
    private BigDecimal totalSpent;
    private String tier;
    private Instant lastActiveAt;
    private Instant joinedAt;
}

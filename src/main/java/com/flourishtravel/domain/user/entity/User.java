package com.flourishtravel.domain.user.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = @Index(columnList = "email", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Ngày sinh – dùng cho khuyến mãi sinh nhật, phân nhóm khách hàng. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Giới tính: male | female | other. */
    @Column(length = 20)
    private String gender;

    /** Địa chỉ chung của khách (cho việc gửi hàng hoá, hoá đơn). */
    @Column(columnDefinition = "TEXT")
    private String address;

    /** Quốc tịch — mặc định Việt Nam. */
    @Column(length = 100)
    private String nationality;

    /** Ghi chú nội bộ chỉ admin xem được. */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    /** KH cho phép nhận email marketing / newsletter (GDPR-friendly). */
    @Column(name = "marketing_opt_in", nullable = false)
    @Builder.Default
    private Boolean marketingOptIn = false;

    /** Thời điểm đăng nhập gần nhất — cập nhật từ AuthService. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Mã nhân viên hiển thị (vd: EMP-A1B2C3D4), sinh khi tạo account nội bộ. */
    @Column(name = "employee_code", unique = true, length = 24)
    private String employeeCode;

    /** Chức danh hiển thị: Sales Tour, Điều hành, Kế toán, Super Admin... */
    @Column(name = "job_title", length = 120)
    private String jobTitle;

    /** Bộ phận (mã ngắn): SALES | OPERATIONS | FINANCE | ADMIN — hoặc GUIDE cho HDV. */
    @Column(length = 40)
    private String department;

    /**
     * Trạng thái làm việc HR: active | on_leave | inactive (nghỉ việc).
     * inactive đồng bộ với không cho đăng nhập khi service cập nhật isActive=false.
     */
    @Column(name = "employment_status", nullable = false, length = 20)
    @Builder.Default
    private String employmentStatus = "active";

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProvider> providers = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();
}

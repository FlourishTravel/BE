package com.flourishtravel.domain.user.repository;

import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByIsActiveTrue();

    /**
     * Danh sách HDV (role = TOUR_GUIDE) đang active, sắp xếp theo tên đầy đủ.
     * Dùng cho trang Điều hành tour (FE chọn HDV thay thế).
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role.name = :roleName
          AND u.isActive = true
        ORDER BY u.fullName ASC
        """)
    List<User> findActiveByRoleName(@Param("roleName") String roleName);

    Optional<User> findByIdAndRole_NameAndIsActiveTrue(UUID id, String roleName);

    /**
     * Tìm kiếm khách hàng cho admin (role = TRAVELER theo mặc định).
     * Truyền pattern dạng "%abc%" (không null) để tránh lỗi Postgres lower(bytea).
     *
     * @param roleName    chỉ trả KH có role này (null = trả tất cả)
     * @param pattern     pattern LIKE (search theo fullName / email / phone)
     * @param active      true / false để lọc; null = không lọc
     */
    @Query("""
        SELECT u FROM User u
        WHERE (:roleName IS NULL OR u.role.name = :roleName)
          AND (:active IS NULL OR u.isActive = :active)
          AND (
                LOWER(COALESCE(u.fullName, '')) LIKE :pattern
             OR LOWER(COALESCE(u.email, ''))    LIKE :pattern
             OR LOWER(COALESCE(u.phone, ''))    LIKE :pattern
          )
        ORDER BY u.createdAt DESC
        """)
    Page<User> adminSearchCustomers(@Param("roleName") String roleName,
                                    @Param("active") Boolean active,
                                    @Param("pattern") String pattern,
                                    Pageable pageable);

    long countByRole_NameAndCreatedAtBetween(String roleName, Instant from, Instant to);

    long countByRole_Name(String roleName);

    /**
     * Danh sách nhân sự nội bộ (không gồm TRAVELER).
     * pattern: "%term%" — không null (dùng "%%" khi không tìm).
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role.name IN ('ADMIN', 'TOUR_GUIDE', 'STAFF')
          AND (:employmentStatus IS NULL OR LOWER(COALESCE(u.employmentStatus, 'active')) = :employmentStatus)
          AND (:roleName IS NULL OR u.role.name = :roleName)
          AND (:department IS NULL OR LOWER(TRIM(COALESCE(u.department, ''))) = :department)
          AND (
                LOWER(COALESCE(u.fullName, '')) LIKE :pattern
             OR LOWER(COALESCE(u.email, ''))    LIKE :pattern
             OR LOWER(COALESCE(u.phone, ''))    LIKE :pattern
             OR LOWER(COALESCE(u.employeeCode, '')) LIKE :pattern
             OR LOWER(COALESCE(u.jobTitle, '')) LIKE :pattern
          )
        ORDER BY u.fullName ASC
        """)
    Page<User> adminSearchStaff(@Param("employmentStatus") String employmentStatus,
                                @Param("roleName") String roleName,
                                @Param("department") String department,
                                @Param("pattern") String pattern,
                                Pageable pageable);

    /** Số tài khoản ADMIN còn đăng nhập được — dùng để không vô hiệu hoá admin cuối cùng. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = 'ADMIN' AND u.isActive = true")
    long countActiveAdmins();
}

package com.flourishtravel.domain.user.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.dto.AdminStaffDetailDto;
import com.flourishtravel.domain.user.dto.AdminStaffSummaryDto;
import com.flourishtravel.domain.user.dto.CreateStaffRequest;
import com.flourishtravel.domain.user.dto.ResetStaffPasswordRequest;
import com.flourishtravel.domain.user.dto.StaffStatsDto;
import com.flourishtravel.domain.user.dto.UpdateStaffRequest;
import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.RoleRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Quản lý nhân sự nội bộ (ADMIN, TOUR_GUIDE, STAFF) — không gồm khách TRAVELER.
 *
 * Quy tắc:
 *  - Không cho vô hiệu hoá hoặc đổi vai khỏi ADMIN nếu đó là admin đang hoạt động cuối cùng.
 *  - employment_status = inactive → đồng bộ is_active = false (không đăng nhập được).
 */
@Service
@RequiredArgsConstructor
public class AdminStaffService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TourSessionRepository tourSessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<AdminStaffSummaryDto> list(String q, String employmentStatus, String roleName,
                                           String department, Pageable pageable) {
        String pattern = buildPattern(q);
        String emp = blankToNull(employmentStatus);
        if (emp != null) {
            emp = emp.toLowerCase(Locale.ROOT);
        }
        String roleFilter = blankToNull(roleName);
        if (roleFilter != null) {
            roleFilter = roleFilter.toUpperCase(Locale.ROOT);
        }
        String deptFilter = blankToNull(department);
        if (deptFilter != null) {
            deptFilter = deptFilter.trim().toLowerCase(Locale.ROOT);
        }

        // Tab "HDV" trên FE gửi department=GUIDE → lọc theo role TOUR_GUIDE.
        if ("GUIDE".equalsIgnoreCase(deptFilter)) {
            roleFilter = "TOUR_GUIDE";
            deptFilter = null;
        }

        Page<User> page = userRepository.adminSearchStaff(emp, roleFilter, deptFilter, pattern, pageable);
        return page.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public StaffStatsDto stats() {
        List<User> staff = userRepository.findAll().stream()
                .filter(u -> isStaffRole(u.getRole()))
                .toList();

        long total = staff.size();

        long active = staff.stream()
                .filter(u -> "active".equalsIgnoreCase(safeEmp(u.getEmploymentStatus()))
                        && Boolean.TRUE.equals(u.getIsActive()))
                .count();

        long onLeave = staff.stream()
                .filter(u -> "on_leave".equalsIgnoreCase(safeEmp(u.getEmploymentStatus())))
                .count();

        long inactive = staff.stream()
                .filter(u -> "inactive".equalsIgnoreCase(safeEmp(u.getEmploymentStatus()))
                        || Boolean.FALSE.equals(u.getIsActive()))
                .count();

        long adminC = countRole(staff, "ADMIN");
        long guideC = countRole(staff, "TOUR_GUIDE");
        long staffC = countRole(staff, "STAFF");

        StaffStatsDto.DeptBreakdown dept = StaffStatsDto.DeptBreakdown.builder()
                .sales(countDept(staff, "SALES"))
                .operations(countDept(staff, "OPERATIONS"))
                .finance(countDept(staff, "FINANCE"))
                .adminDept(countDept(staff, "ADMIN"))
                .guides(guideC)
                .other(countOtherDept(staff))
                .build();

        return StaffStatsDto.builder()
                .totalStaff(total)
                .activeCount(active)
                .onLeaveCount(onLeave)
                .inactiveCount(inactive)
                .adminCount(adminC)
                .guideCount(guideC)
                .internalStaffCount(staffC)
                .byDepartment(dept)
                .build();
    }

    private long countRole(List<User> staff, String roleName) {
        return staff.stream()
                .filter(u -> u.getRole() != null && roleName.equalsIgnoreCase(u.getRole().getName()))
                .count();
    }

    private long countDept(List<User> staff, String deptCode) {
        return staff.stream()
                .filter(u -> deptCode.equalsIgnoreCase(blankToEmpty(u.getDepartment())))
                .count();
    }

    /** HDV không có department cụ thể hoặc khác các bucket chuẩn. */
    private long countOtherDept(List<User> staff) {
        return staff.stream()
                .filter(u -> {
                    String d = blankToEmpty(u.getDepartment());
                    if (d.isEmpty()) {
                        return !"TOUR_GUIDE".equalsIgnoreCase(u.getRole() != null ? u.getRole().getName() : "");
                    }
                    return !d.equalsIgnoreCase("SALES") && !d.equalsIgnoreCase("OPERATIONS")
                            && !d.equalsIgnoreCase("FINANCE") && !d.equalsIgnoreCase("ADMIN")
                            && !d.equalsIgnoreCase("GUIDE");
                })
                .count();
    }

    private static boolean isStaffRole(Role role) {
        if (role == null) return false;
        String n = role.getName();
        return "ADMIN".equalsIgnoreCase(n) || "TOUR_GUIDE".equalsIgnoreCase(n) || "STAFF".equalsIgnoreCase(n);
    }

    private static String safeEmp(String s) {
        return (s == null || s.isBlank()) ? "active" : s.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public AdminStaffDetailDto detail(UUID id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        if (!isStaffRole(u.getRole())) {
            throw new BadRequestException("Người dùng này không thuộc nhân sự nội bộ");
        }
        return toDetail(u);
    }

    @Transactional
    public AdminStaffDetailDto create(CreateStaffRequest req) {
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng");
        }
        Role role = roleRepository.findByName(req.getRoleName().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ"));
        if ("TRAVELER".equalsIgnoreCase(role.getName())) {
            throw new BadRequestException("Không thể tạo nhân viên với vai Khách hàng");
        }
        if (!isStaffRole(role)) {
            throw new BadRequestException("Chỉ được gán vai ADMIN, TOUR_GUIDE hoặc STAFF");
        }

        User u = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName().trim())
                .phone(blankToNull(req.getPhone()))
                .role(role)
                .isActive(true)
                .marketingOptIn(false)
                .employmentStatus(req.getEmploymentStatus() == null || req.getEmploymentStatus().isBlank()
                        ? "active"
                        : req.getEmploymentStatus().trim().toLowerCase(Locale.ROOT))
                .jobTitle(blankToNull(req.getJobTitle()))
                .department(blankToNull(req.getDepartment()))
                .build();

        u = userRepository.save(u);
        u.setEmployeeCode(generateEmployeeCode(u.getId()));
        u = userRepository.save(u);

        return toDetail(u);
    }

    @Transactional
    public AdminStaffDetailDto update(UUID id, UpdateStaffRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        if (!isStaffRole(u.getRole())) {
            throw new BadRequestException("Không phải nhân viên nội bộ");
        }

        boolean wasAdmin = u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().getName());

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            u.setFullName(req.getFullName().trim());
        }
        if (req.getPhone() != null) {
            u.setPhone(req.getPhone().isBlank() ? null : req.getPhone().trim());
        }
        if (req.getAvatarUrl() != null) {
            u.setAvatarUrl(req.getAvatarUrl().isBlank() ? null : req.getAvatarUrl().trim());
        }
        if (req.getDateOfBirth() != null) {
            u.setDateOfBirth(req.getDateOfBirth());
        }
        if (req.getGender() != null) {
            u.setGender(req.getGender().isBlank() ? null : req.getGender().trim().toLowerCase(Locale.ROOT));
        }
        if (req.getAddress() != null) {
            u.setAddress(req.getAddress().isBlank() ? null : req.getAddress().trim());
        }
        if (req.getJobTitle() != null) {
            u.setJobTitle(req.getJobTitle().isBlank() ? null : req.getJobTitle().trim());
        }
        if (req.getDepartment() != null) {
            u.setDepartment(req.getDepartment().isBlank() ? null : req.getDepartment().trim().toUpperCase(Locale.ROOT));
        }
        if (req.getAdminNote() != null) {
            u.setAdminNote(req.getAdminNote().isBlank() ? null : req.getAdminNote().trim());
        }

        if (req.getRoleName() != null && !req.getRoleName().isBlank()) {
            Role newRole = roleRepository.findByName(req.getRoleName().trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ"));
            if (!isStaffRole(newRole)) {
                throw new BadRequestException("Không được gán vai Khách hàng cho nhân viên");
            }
            if (wasAdmin && !"ADMIN".equalsIgnoreCase(newRole.getName())) {
                assertNotLastActiveAdmin(u.getId());
            }
            u.setRole(newRole);
        }

        if (req.getEmploymentStatus() != null && !req.getEmploymentStatus().isBlank()) {
            String next = req.getEmploymentStatus().trim().toLowerCase(Locale.ROOT);
            u.setEmploymentStatus(next);
            if ("inactive".equals(next)) {
                assertNotLastActiveAdmin(u.getId());
                u.setIsActive(false);
            } else {
                u.setIsActive(true);
            }
        }

        userRepository.save(u);
        return toDetail(u);
    }

    @Transactional
    public AdminStaffDetailDto setActive(UUID id, boolean active) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        if (!isStaffRole(u.getRole())) {
            throw new BadRequestException("Không phải nhân viên nội bộ");
        }
        if (!active) {
            assertNotLastActiveAdmin(id);
            u.setEmploymentStatus("inactive");
            u.setIsActive(false);
        } else {
            u.setIsActive(true);
            if ("inactive".equalsIgnoreCase(safeEmp(u.getEmploymentStatus()))) {
                u.setEmploymentStatus("active");
            }
        }
        userRepository.save(u);
        return toDetail(u);
    }

    @Transactional
    public AdminStaffDetailDto resetPassword(UUID id, ResetStaffPasswordRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        if (!isStaffRole(u.getRole())) {
            throw new BadRequestException("Không phải nhân viên nội bộ");
        }
        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);
        return toDetail(u);
    }

    private void assertNotLastActiveAdmin(UUID userId) {
        User u = userRepository.findById(userId).orElseThrow();
        if (u.getRole() == null || !"ADMIN".equalsIgnoreCase(u.getRole().getName())) {
            return;
        }
        if (!Boolean.TRUE.equals(u.getIsActive())) {
            return;
        }
        long admins = userRepository.countActiveAdmins();
        if (admins <= 1) {
            throw new BadRequestException("Không thể thao tác: đây là tài khoản ADMIN đang hoạt động duy nhất");
        }
    }

    private AdminStaffSummaryDto toSummary(User u) {
        Role r = u.getRole();
        String rn = r != null ? r.getName() : "";
        return AdminStaffSummaryDto.builder()
                .id(u.getId())
                .employeeCode(u.getEmployeeCode())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .roleName(rn)
                .roleLabel(roleLabel(rn))
                .jobTitle(u.getJobTitle())
                .department(u.getDepartment())
                .departmentLabel(departmentLabel(u.getDepartment()))
                .employmentStatus(safeEmp(u.getEmploymentStatus()))
                .active(Boolean.TRUE.equals(u.getIsActive()))
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }

    private AdminStaffDetailDto toDetail(User u) {
        Role r = u.getRole();
        String rn = r != null ? r.getName() : "";

        LocalDate today = LocalDate.now(ZONE_VN);
        LocalDate end = today.plusDays(90);

        long upcoming = 0;
        long next90 = 0;
        if ("TOUR_GUIDE".equalsIgnoreCase(rn)) {
            upcoming = tourSessionRepository.countUpcomingForGuide(u.getId(), today);
            next90 = tourSessionRepository.countScheduledForGuideBetween(u.getId(), today, end);
        }

        return AdminStaffDetailDto.builder()
                .id(u.getId())
                .employeeCode(u.getEmployeeCode())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .dateOfBirth(u.getDateOfBirth())
                .gender(u.getGender())
                .address(u.getAddress())
                .roleName(rn)
                .roleLabel(roleLabel(rn))
                .jobTitle(u.getJobTitle())
                .department(u.getDepartment())
                .departmentLabel(departmentLabel(u.getDepartment()))
                .employmentStatus(safeEmp(u.getEmploymentStatus()))
                .active(Boolean.TRUE.equals(u.getIsActive()))
                .adminNote(u.getAdminNote())
                .lastLoginAt(u.getLastLoginAt())
                .joinedAt(u.getCreatedAt())
                .upcomingSessionsCount(upcoming)
                .scheduledSessionsNext90Days((long) next90)
                .build();
    }

    private static String generateEmployeeCode(UUID id) {
        return "EMP-" + id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String roleLabel(String roleName) {
        if (roleName == null) return "";
        return switch (roleName.toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> "Quản trị hệ thống";
            case "TOUR_GUIDE" -> "Hướng dẫn viên";
            case "STAFF" -> "Nhân viên nội bộ";
            default -> roleName;
        };
    }

    private static String departmentLabel(String dept) {
        if (dept == null || dept.isBlank()) return "—";
        return switch (dept.toUpperCase(Locale.ROOT)) {
            case "SALES" -> "Sales / Tour";
            case "OPERATIONS" -> "Điều hành";
            case "FINANCE" -> "Kế toán";
            case "ADMIN" -> "Quản trị";
            case "GUIDE" -> "Hướng dẫn viên";
            default -> dept;
        };
    }

    private static String buildPattern(String q) {
        String term = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        return "%" + term + "%";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank() || "all".equalsIgnoreCase(s)) ? null : s.trim();
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}

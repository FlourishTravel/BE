package com.flourishtravel.domain.user.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.dto.AdminCustomerDetailDto;
import com.flourishtravel.domain.user.dto.AdminCustomerSummaryDto;
import com.flourishtravel.domain.user.dto.AdminUpdateCustomerRequest;
import com.flourishtravel.domain.user.dto.CustomerStatsDto;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.entity.UserFavorite;
import com.flourishtravel.domain.user.repository.UserFavoriteRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ Quản Lý Khách Hàng cho admin.
 *
 * Quy tắc hạng (tier) — tính theo tổng số tiền KH đã thanh toán thành công:
 *  - VIP      : ≥ 100,000,000 VND
 *  - GOLD     : ≥ 50,000,000 VND
 *  - SILVER   : ≥ 20,000,000 VND
 *  - STANDARD : còn lại
 *
 * Phạm vi "khách hàng" = User có role TRAVELER. Admin & Tour Guide bị loại trừ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCustomerService {

    private static final String ROLE_TRAVELER = "TRAVELER";
    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final BigDecimal TIER_VIP_THRESHOLD = new BigDecimal("100000000");
    private static final BigDecimal TIER_GOLD_THRESHOLD = new BigDecimal("50000000");
    private static final BigDecimal TIER_SILVER_THRESHOLD = new BigDecimal("20000000");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserFavoriteRepository userFavoriteRepository;

    // ---------- LIST ----------

    @Transactional(readOnly = true)
    public Page<AdminCustomerSummaryDto> adminList(String q, String tier, Boolean active, Pageable pageable) {
        String term = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + term + "%";

        Page<User> users = userRepository.adminSearchCustomers(ROLE_TRAVELER, active, pattern, pageable);

        List<AdminCustomerSummaryDto> dtos = users.getContent().stream()
                .map(this::toSummary)
                .toList();

        // Lọc tier sau cùng (vì tier suy luận từ totalSpent — không lưu DB).
        if (tier != null && !tier.isBlank() && !"all".equalsIgnoreCase(tier)) {
            String normTier = tier.trim().toUpperCase(Locale.ROOT);
            dtos = dtos.stream().filter(d -> normTier.equals(d.getTier())).toList();
        }

        return new PageImpl<>(dtos, pageable, users.getTotalElements());
    }

    // ---------- DETAIL ----------

    @Transactional(readOnly = true)
    public AdminCustomerDetailDto adminDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", userId));

        BigDecimal totalSpent = bookingRepository.sumPaidByUser(userId);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;
        long bookingCount = bookingRepository.countByUser_Id(userId);
        long completedCount = bookingRepository.countByUser_IdAndStatus(userId, "completed");
        long cancelledCount = bookingRepository.countByUser_IdAndStatus(userId, "cancelled");
        BigDecimal pendingRefund = bookingRepository.sumPendingRefundByUser(userId);
        if (pendingRefund == null) pendingRefund = BigDecimal.ZERO;
        Instant lastBookingAt = bookingRepository.findLastBookingAtByUser(userId);

        // Recent bookings (tối đa 10).
        List<Booking> recent = bookingRepository.findRecentByUser(userId, PageRequest.of(0, 10));
        List<AdminCustomerDetailDto.BookingRef> bookingRefs = recent.stream()
                .map(this::toBookingRef)
                .toList();

        // Favorites (tối đa 20).
        List<AdminCustomerDetailDto.FavoriteRef> favoriteRefs = userFavoriteRepository
                .findByUserOrderByCreatedAtDesc(user).stream()
                .limit(20)
                .map(this::toFavoriteRef)
                .toList();

        // Activities (suy luận từ booking + favorite + tài khoản).
        List<AdminCustomerDetailDto.ActivityRef> activities = buildActivities(user, recent, favoriteRefs);

        BigDecimal aov = bookingCount > 0
                ? totalSpent.divide(BigDecimal.valueOf(bookingCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Instant joinedAt = user.getCreatedAt();
        Integer membershipMonths = (joinedAt == null) ? null
                : (int) java.time.temporal.ChronoUnit.MONTHS.between(
                        joinedAt.atZone(ZONE_VN).toLocalDate().withDayOfMonth(1),
                        LocalDate.now(ZONE_VN).withDayOfMonth(1));

        Integer age = calcAge(user.getDateOfBirth());
        Instant lastActiveAt = pickLastActive(user, lastBookingAt);

        return AdminCustomerDetailDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .marketingOptIn(Boolean.TRUE.equals(user.getMarketingOptIn()))
                .dateOfBirth(user.getDateOfBirth())
                .age(age)
                .gender(user.getGender())
                .address(user.getAddress())
                .nationality(user.getNationality())
                .adminNote(user.getAdminNote())
                .lastLoginAt(user.getLastLoginAt())
                .bookingCount(bookingCount)
                .completedBookingCount(completedCount)
                .cancelledBookingCount(cancelledCount)
                .totalSpent(totalSpent)
                .averageOrderValue(aov)
                .pendingRefundAmount(pendingRefund)
                .tier(deriveTier(totalSpent))
                .joinedAt(joinedAt)
                .lastActiveAt(lastActiveAt)
                .membershipMonths(membershipMonths != null && membershipMonths >= 0 ? membershipMonths : null)
                .recentBookings(bookingRefs)
                .favoriteTours(favoriteRefs)
                .activities(activities)
                .build();
    }

    // ---------- STATS ----------

    @Transactional(readOnly = true)
    public CustomerStatsDto stats() {
        LocalDate now = LocalDate.now(ZONE_VN);
        YearMonth ym = YearMonth.from(now);
        Instant monthStart = ym.atDay(1).atStartOfDay(ZONE_VN).toInstant();
        Instant monthEnd = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE_VN).toInstant();

        long total = userRepository.countByRole_Name(ROLE_TRAVELER);
        long newThisMonth = userRepository.countByRole_NameAndCreatedAtBetween(ROLE_TRAVELER, monthStart, monthEnd);

        // Phân bố tier (lấy toàn bộ TRAVELER — chấp nhận chi phí nhẹ cho dashboard).
        // Trong môi trường lớn, nên cache hoặc dùng materialized view.
        Map<String, Long> byTier = userRepository
                .adminSearchCustomers(ROLE_TRAVELER, null, "%%", PageRequest.of(0, Integer.MAX_VALUE))
                .getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        u -> deriveTier(safeSum(bookingRepository.sumPaidByUser(u.getId()))),
                        java.util.stream.Collectors.counting()
                ));

        long vip = byTier.getOrDefault("VIP", 0L);
        long gold = byTier.getOrDefault("GOLD", 0L);
        long silver = byTier.getOrDefault("SILVER", 0L);
        long standard = byTier.getOrDefault("STANDARD", 0L);

        // Tỷ lệ quay lại = KH có ≥2 booking / KH có ≥1 booking.
        long customersWithBookings = 0;
        long repeatCustomers = 0;
        BigDecimal totalAllPaid = BigDecimal.ZERO;
        long spentCustomers = 0;
        for (User u : userRepository.adminSearchCustomers(ROLE_TRAVELER, null, "%%", PageRequest.of(0, Integer.MAX_VALUE)).getContent()) {
            long bc = bookingRepository.countByUser_Id(u.getId());
            if (bc >= 1) customersWithBookings++;
            if (bc >= 2) repeatCustomers++;
            BigDecimal paid = safeSum(bookingRepository.sumPaidByUser(u.getId()));
            if (paid.signum() > 0) {
                spentCustomers++;
                totalAllPaid = totalAllPaid.add(paid);
            }
        }

        double returnRate = customersWithBookings == 0 ? 0.0
                : Math.round((repeatCustomers * 100.0 / customersWithBookings) * 10.0) / 10.0;

        BigDecimal avgSpend = spentCustomers == 0
                ? BigDecimal.ZERO
                : totalAllPaid.divide(BigDecimal.valueOf(spentCustomers), 0, RoundingMode.HALF_UP);

        return CustomerStatsDto.builder()
                .totalCustomers(total)
                .newCustomersThisMonth(newThisMonth)
                .vipCustomers(vip)
                .returnRatePercent(returnRate)
                .averageSpendPerCustomer(avgSpend)
                .breakdown(CustomerStatsDto.TierBreakdown.builder()
                        .vip(vip).gold(gold).silver(silver).standard(standard)
                        .build())
                .build();
    }

    // ---------- UPDATE / STATUS ----------

    @Transactional
    public AdminCustomerDetailDto updateCustomer(UUID userId, AdminUpdateCustomerRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", userId));

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new BadRequestException("Email đã được sử dụng bởi tài khoản khác");
            }
            user.setEmail(newEmail);
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone().isBlank() ? null : req.getPhone().trim());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl().isBlank() ? null : req.getAvatarUrl().trim());
        }
        if (req.getDateOfBirth() != null) {
            user.setDateOfBirth(req.getDateOfBirth());
        }
        if (req.getGender() != null) {
            user.setGender(req.getGender().isBlank() ? null : req.getGender().trim().toLowerCase(Locale.ROOT));
        }
        if (req.getAddress() != null) {
            user.setAddress(req.getAddress().isBlank() ? null : req.getAddress().trim());
        }
        if (req.getNationality() != null) {
            user.setNationality(req.getNationality().isBlank() ? null : req.getNationality().trim());
        }
        if (req.getAdminNote() != null) {
            user.setAdminNote(req.getAdminNote().isBlank() ? null : req.getAdminNote().trim());
        }
        if (req.getMarketingOptIn() != null) {
            user.setMarketingOptIn(req.getMarketingOptIn());
        }

        userRepository.save(user);
        return adminDetail(user.getId());
    }

    @Transactional
    public AdminCustomerDetailDto setActive(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", userId));
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName()) && !active) {
            throw new BadRequestException("Không thể vô hiệu hoá tài khoản ADMIN từ trang Khách Hàng");
        }
        user.setIsActive(active);
        userRepository.save(user);
        return adminDetail(user.getId());
    }

    // ---------- Helpers ----------

    private AdminCustomerSummaryDto toSummary(User u) {
        BigDecimal totalSpent = safeSum(bookingRepository.sumPaidByUser(u.getId()));
        long bookingCount = bookingRepository.countByUser_Id(u.getId());
        Instant lastBookingAt = bookingRepository.findLastBookingAtByUser(u.getId());

        return AdminCustomerSummaryDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole() != null ? u.getRole().getName() : null)
                .active(Boolean.TRUE.equals(u.getIsActive()))
                .marketingOptIn(Boolean.TRUE.equals(u.getMarketingOptIn()))
                .bookingCount(bookingCount)
                .totalSpent(totalSpent)
                .tier(deriveTier(totalSpent))
                .lastActiveAt(pickLastActive(u, lastBookingAt))
                .joinedAt(u.getCreatedAt())
                .build();
    }

    private AdminCustomerDetailDto.BookingRef toBookingRef(Booking b) {
        TourSession session = b.getSession();
        Tour tour = session != null ? session.getTour() : null;
        BigDecimal paid = b.getPayments() == null ? BigDecimal.ZERO
                : b.getPayments().stream()
                .filter(p -> "paid".equalsIgnoreCase(p.getStatus()))
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminCustomerDetailDto.BookingRef.builder()
                .id(b.getId())
                .bookingCode(buildBookingCode(b.getId()))
                .status(b.getStatus())
                .tourTitle(tour != null ? tour.getTitle() : null)
                .startDate(session != null ? session.getStartDate() : null)
                .guestCount(b.getGuestCount())
                .totalAmount(b.getTotalAmount())
                .paidAmount(paid)
                .createdAt(b.getCreatedAt())
                .build();
    }

    private AdminCustomerDetailDto.FavoriteRef toFavoriteRef(UserFavorite f) {
        Tour tour = f.getTour();
        String thumb = null;
        if (tour != null && tour.getImages() != null) {
            thumb = tour.getImages().stream()
                    .min(Comparator.comparingInt(img -> img.getSortOrder() == null ? 9999 : img.getSortOrder()))
                    .map(TourImage::getImageUrl)
                    .orElse(null);
        }
        return AdminCustomerDetailDto.FavoriteRef.builder()
                .tourId(tour != null ? tour.getId() : null)
                .tourTitle(tour != null ? tour.getTitle() : null)
                .thumbnailUrl(thumb)
                .addedAt(f.getCreatedAt())
                .build();
    }

    private List<AdminCustomerDetailDto.ActivityRef> buildActivities(
            User user, List<Booking> recent, List<AdminCustomerDetailDto.FavoriteRef> favorites) {

        List<AdminCustomerDetailDto.ActivityRef> activities = new ArrayList<>();

        if (user.getCreatedAt() != null) {
            activities.add(AdminCustomerDetailDto.ActivityRef.builder()
                    .type("account_created")
                    .text("Đăng ký tài khoản")
                    .at(user.getCreatedAt())
                    .build());
        }

        for (Booking b : recent) {
            String tourTitle = b.getSession() != null && b.getSession().getTour() != null
                    ? b.getSession().getTour().getTitle()
                    : "tour";
            String status = b.getStatus() == null ? "" : b.getStatus().toLowerCase(Locale.ROOT);
            String prefix;
            String type;
            switch (status) {
                case "cancelled" -> { prefix = "Huỷ booking"; type = "booking_cancelled"; }
                case "completed" -> { prefix = "Hoàn thành tour"; type = "booking_completed"; }
                case "paid"      -> { prefix = "Thanh toán booking"; type = "booking_paid"; }
                case "confirmed" -> { prefix = "Xác nhận booking"; type = "booking_confirmed"; }
                default          -> { prefix = "Đặt tour"; type = "booking_created"; }
            }
            activities.add(AdminCustomerDetailDto.ActivityRef.builder()
                    .type(type)
                    .text(prefix + " " + tourTitle)
                    .at(b.getCreatedAt())
                    .build());
        }

        for (AdminCustomerDetailDto.FavoriteRef f : favorites) {
            activities.add(AdminCustomerDetailDto.ActivityRef.builder()
                    .type("favorite_added")
                    .text("Thêm yêu thích: " + (f.getTourTitle() == null ? "" : f.getTourTitle()))
                    .at(f.getAddedAt())
                    .build());
        }

        activities.sort(Comparator.comparing(
                AdminCustomerDetailDto.ActivityRef::getAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return activities.stream().limit(12).toList();
    }

    private static String deriveTier(BigDecimal totalSpent) {
        if (totalSpent == null) return "STANDARD";
        if (totalSpent.compareTo(TIER_VIP_THRESHOLD) >= 0) return "VIP";
        if (totalSpent.compareTo(TIER_GOLD_THRESHOLD) >= 0) return "GOLD";
        if (totalSpent.compareTo(TIER_SILVER_THRESHOLD) >= 0) return "SILVER";
        return "STANDARD";
    }

    private static BigDecimal safeSum(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static Instant pickLastActive(User user, Instant lastBookingAt) {
        Instant a = user.getLastLoginAt();
        Instant b = lastBookingAt;
        Instant c = user.getUpdatedAt();
        Instant best = a;
        if (b != null && (best == null || b.isAfter(best))) best = b;
        if (c != null && (best == null || c.isAfter(best))) best = c;
        return best;
    }

    private static Integer calcAge(LocalDate dob) {
        if (dob == null) return null;
        return Period.between(dob, LocalDate.now(ZONE_VN)).getYears();
    }

    private static String buildBookingCode(UUID id) {
        if (id == null) return null;
        return "FT-" + id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}

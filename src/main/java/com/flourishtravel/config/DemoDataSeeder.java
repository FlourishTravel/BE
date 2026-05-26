package com.flourishtravel.config;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.booking.entity.SessionParticipant;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.booking.repository.SessionParticipantRepository;
import com.flourishtravel.domain.booking.service.SessionParticipantSyncService;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import com.flourishtravel.domain.payment.repository.RefundRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.RoleRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Seed dữ liệu DEMO cho trang admin:
 *   - 8 tài khoản hướng dẫn viên (role TOUR_GUIDE)
 *   - 15 khách hàng (role TRAVELER) với tier phân bổ thực tế
 *   - 25+ booking trải nhiều status (pending / paid / confirmed / completed / cancelled)
 *   - Payments tương ứng (paid / partial / refunded)
 *   - 2 refund pending (KH yêu cầu hoàn tiền)
 *   - Gán HDV cho một số session để dùng cho trang Tour Operations
 *   - Đồng bộ session_participants từ booking paid khi bảng còn trống (backfill + demo điểm danh)
 *
 * Idempotent: nếu đã có > 5 booking thì bỏ qua seed booking hàng loạt; luôn bổ sung đơn cho {@code traveler@example.com}
 * nếu user đó chưa có booking (để trang "Chuyến đi của tôi" có dữ liệu khi đăng nhập Traveler@123).
 * Participants chỉ seed khi chưa có dòng nào.
 *
 * Mật khẩu mặc định cho mọi tài khoản seed: "Demo@123".
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TourRepository tourRepository;
    private final TourSessionRepository tourSessionRepository;
    private final BookingRepository bookingRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionParticipantSyncService sessionParticipantSyncService;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "Demo@123";

    @EventListener(ApplicationReadyEvent.class)
    @Order(5)
    @Transactional
    public void seed() {
        Role guideRole = roleRepository.findByName("TOUR_GUIDE")
                .orElseThrow(() -> new IllegalStateException("Role TOUR_GUIDE not found. Run RoleSeeder first."));
        Role travelerRole = roleRepository.findByName("TRAVELER")
                .orElseThrow(() -> new IllegalStateException("Role TRAVELER not found."));
        Role staffRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new IllegalStateException("Role STAFF not found."));

        List<Tour> tours = tourRepository.findAll();
        if (tours.isEmpty()) {
            log.warn("No tours found. Run TourSeeder first. Skip demo data.");
            return;
        }

        // 1) Tour guides — luôn idempotent, bỏ qua HDV đã có email.
        List<User> guides = seedTourGuides(guideRole);

        // 1b) Nhân viên nội bộ (sales / điều hành / kế toán) — demo tab phòng ban trên admin.
        seedInternalStaff(staffRole);

        // 2) Customers — luôn idempotent, bỏ qua KH đã có email.
        List<User> customers = seedCustomers(travelerRole);

        // 3) Assign guides to some upcoming sessions (nếu session chưa có HDV).
        assignGuidesToSessions(guides);

        // 4) Bookings — chỉ seed khi count thấp để tránh nhân bản.
        long bookingCount = bookingRepository.count();
        if (bookingCount > 5) {
            log.info("Bookings already present ({}). Skip booking seed.", bookingCount);
        } else {
            seedBookings(customers, tours);
        }

        ensureDefaultTravelerHasDemoBookings();

        seedSessionParticipantsIfEmpty();

        log.info("Demo data seeded: {} guides, {} customers, {} bookings, {} session participants (login: <email> / {})",
                guides.size(), customers.size(), bookingRepository.count(),
                sessionParticipantRepository.count(), DEFAULT_PASSWORD);
    }

    /**
     * Khi bảng session_participants trống: đồng bộ từ mọi booking paid (giống luồng production),
     * rồi gán check-in/out demo để HDV thấy trạng thái hỗn hợp.
     */
    private void seedSessionParticipantsIfEmpty() {
        long existing = sessionParticipantRepository.count();
        if (existing > 0) {
            log.info("session_participants already present ({}). Skip participant sync seed.", existing);
            return;
        }

        List<Booking> paid = bookingRepository.findAllByStatusIgnoreCase("paid");
        if (paid.isEmpty()) {
            log.warn("No paid bookings found. Skip session_participants seed.");
            return;
        }

        for (Booking b : paid) {
            sessionParticipantSyncService.syncPaidBooking(b.getId());
        }
        log.info("Synced session_participants from {} paid booking(s).", paid.size());
        enrichDemoParticipantAttendance();
    }

    private void enrichDemoParticipantAttendance() {
        List<SessionParticipant> rows = sessionParticipantRepository.findAll();
        if (rows.isEmpty()) {
            return;
        }
        Random rnd = new Random(42);
        Instant base = Instant.now();
        for (SessionParticipant p : rows) {
            if (rnd.nextDouble() < 0.65) {
                p.setCheckInAt(base.minus(rnd.nextInt(180), ChronoUnit.MINUTES));
                if (rnd.nextDouble() < 0.4 && p.getCheckInAt() != null) {
                    p.setCheckOutAt(p.getCheckInAt().plus(30 + rnd.nextInt(360), ChronoUnit.MINUTES));
                }
            }
            sessionParticipantRepository.save(p);
        }
        log.info("Applied demo check-in/out to {} session participant row(s).", rows.size());
    }

    // ---------------- Tour Guides ----------------

    private List<User> seedTourGuides(Role guideRole) {
        List<GuideDef> defs = List.of(
                new GuideDef("Trần Quang Minh",  "minh.tran@flourishtravel.com",     "0911222333", "male",
                        LocalDate.of(1988, 4, 12),  "Hà Nội",
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200"),
                new GuideDef("Lê Thị Hồng",      "hong.le@flourishtravel.com",       "0911444555", "female",
                        LocalDate.of(1992, 8, 25),  "TP. Hồ Chí Minh",
                        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200"),
                new GuideDef("Phạm Văn Hùng",    "hung.pham@flourishtravel.com",     "0911666777", "male",
                        LocalDate.of(1985, 1, 30),  "Đà Nẵng",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"),
                new GuideDef("Nguyễn Thị Lan",   "lan.nguyen@flourishtravel.com",    "0911888999", "female",
                        LocalDate.of(1990, 11, 5),  "TP. Hồ Chí Minh",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"),
                new GuideDef("Hoàng Minh Khôi",  "khoi.hoang@flourishtravel.com",    "0922111333", "male",
                        LocalDate.of(1993, 6, 18),  "Hà Nội",
                        "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200"),
                new GuideDef("Bùi Thanh Mai",    "mai.bui@flourishtravel.com",       "0922222444", "female",
                        LocalDate.of(1991, 3, 22),  "Nha Trang",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"),
                new GuideDef("Đặng Quốc Bảo",    "bao.dang@flourishtravel.com",      "0922333555", "male",
                        LocalDate.of(1987, 9, 8),   "Hà Nội",
                        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200"),
                new GuideDef("Vũ Thu Phương",    "phuong.vu@flourishtravel.com",     "0922444666", "female",
                        LocalDate.of(1995, 12, 14), "TP. Hồ Chí Minh",
                        "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200")
        );

        List<User> result = new ArrayList<>();
        for (GuideDef d : defs) {
            User existing = userRepository.findByEmail(d.email).orElse(null);
            if (existing != null) { result.add(existing); continue; }

            User u = User.builder()
                    .email(d.email)
                    .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .fullName(d.fullName)
                    .phone(d.phone)
                    .avatarUrl(d.avatarUrl)
                    .gender(d.gender)
                    .dateOfBirth(d.dob)
                    .address(d.address)
                    .nationality("Việt Nam")
                    .jobTitle("Hướng dẫn viên")
                    .department("GUIDE")
                    .employmentStatus("active")
                    .role(guideRole)
                    .isActive(true)
                    .marketingOptIn(false)
                    .lastLoginAt(Instant.now().minus(rand(1, 7), ChronoUnit.DAYS))
                    .adminNote("HDV — đã được duyệt hồ sơ. Liên hệ qua email công ty.")
                    .build();
            u = userRepository.save(u);
            u.setEmployeeCode(employeeCodeFromId(u.getId()));
            result.add(userRepository.save(u));
        }
        log.info("Seeded {} tour guides", result.size());
        return result;
    }

    /** Nhân viên STAFF (sales / điều hành / kế toán) — bỏ qua nếu email đã tồn tại. */
    private void seedInternalStaff(Role staffRole) {
        record StaffDef(String fullName, String email, String phone, String dept, String jobTitle, String employmentStatus) { }

        List<StaffDef> defs = List.of(
                new StaffDef("Nguyễn Trần Minh", "sales.demo@flourishtravel.com", "0933111222", "SALES", "Sales Tour", "active"),
                new StaffDef("Trần Hương Ly", "ops.demo@flourishtravel.com", "0933222333", "OPERATIONS", "Điều hành tour", "active"),
                new StaffDef("Phạm Thanh Mai", "finance.demo@flourishtravel.com", "0933333444", "FINANCE", "Kế toán", "on_leave")
        );

        int added = 0;
        for (StaffDef d : defs) {
            if (userRepository.findByEmail(d.email()).isPresent()) {
                continue;
            }
            User u = User.builder()
                    .email(d.email())
                    .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .fullName(d.fullName())
                    .phone(d.phone())
                    .jobTitle(d.jobTitle())
                    .department(d.dept())
                    .employmentStatus(d.employmentStatus().toLowerCase(Locale.ROOT))
                    .role(staffRole)
                    .isActive(true)
                    .marketingOptIn(false)
                    .lastLoginAt(Instant.now().minus(rand(1, 14), ChronoUnit.DAYS))
                    .adminNote("Tài khoản demo nội bộ — trang Quản lý nhân viên.")
                    .build();
            u = userRepository.save(u);
            u.setEmployeeCode(employeeCodeFromId(u.getId()));
            userRepository.save(u);
            added++;
        }
        log.info("Seeded {} internal STAFF users (demo; skipped if email exists)", added);
    }

    // ---------------- Customers ----------------

    private List<User> seedCustomers(Role travelerRole) {
        List<CustomerDef> defs = List.of(
                // VIP (3) — sẽ tạo nhiều booking đắt
                new CustomerDef("Nguyễn Văn An",        "an.nguyen@email.com",      "0901234567", "male",
                        LocalDate.of(1982, 3, 15), "123 Lê Lợi, Q.1, TP.HCM",
                        "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200", true,  Tier.VIP),
                new CustomerDef("Lê Minh Châu",         "chau.le@email.com",         "0923456789", "female",
                        LocalDate.of(1979, 7, 22), "456 Trần Hưng Đạo, Q.5, TP.HCM",
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200", true,  Tier.VIP),
                new CustomerDef("Đỗ Quang Vinh",        "vinh.do@email.com",         "0935678901", "male",
                        LocalDate.of(1975, 12, 8), "789 Nguyễn Trãi, Hà Nội",
                        "https://images.unsplash.com/photo-1545167622-3a6ac756afa4?w=200",  true, Tier.VIP),

                // Gold (3)
                new CustomerDef("Trần Thị Bình",        "binh.tran@email.com",       "0912345678", "female",
                        LocalDate.of(1988, 5, 10), "12 Hai Bà Trưng, Q.3, TP.HCM",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200", true, Tier.GOLD),
                new CustomerDef("Hoàng Thị Em",         "em.hoang@email.com",        "0945678901", "female",
                        LocalDate.of(1991, 9, 18), "34 Pasteur, Q.1, TP.HCM",
                        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200", false, Tier.GOLD),
                new CustomerDef("Đặng Thu Hương",       "huong.dang@email.com",      "0967890123", "female",
                        LocalDate.of(1985, 2, 27), "56 Trần Phú, Đà Nẵng",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200", true, Tier.GOLD),

                // Silver (4)
                new CustomerDef("Phạm Đức Duy",         "duy.pham@email.com",        "0934567890", "male",
                        LocalDate.of(1995, 11, 3), "78 Võ Văn Tần, Q.3, TP.HCM",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200", true, Tier.SILVER),
                new CustomerDef("Bùi Văn Khoa",         "khoa.bui@email.com",        "0978901234", "male",
                        LocalDate.of(1992, 6, 14), "90 Điện Biên Phủ, Q.10, TP.HCM",
                        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200", false, Tier.SILVER),
                new CustomerDef("Trương Mỹ Linh",       "linh.truong@email.com",     "0989012345", "female",
                        LocalDate.of(1996, 4, 7),  "12 Lý Tự Trọng, Q.1, TP.HCM",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200", true, Tier.SILVER),
                new CustomerDef("Vũ Quang Huy",         "huy.vu@email.com",          "0956789012", "male",
                        LocalDate.of(1993, 8, 20), "34 Phan Đăng Lưu, Q. Bình Thạnh, TP.HCM",
                        "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200", true, Tier.SILVER),

                // Standard (5) — chưa hoặc mới có 1 booking nhỏ
                new CustomerDef("Ngô Thanh Tùng",       "tung.ngo@email.com",        "0901112233", "male",
                        LocalDate.of(1998, 1, 25), "56 Cách Mạng Tháng 8, Q.10, TP.HCM",
                        null, true, Tier.STANDARD),
                new CustomerDef("Lý Bảo Yến",           "yen.ly@email.com",          "0901334455", "female",
                        LocalDate.of(2000, 10, 11), "78 Lê Đại Hành, Q.11, TP.HCM",
                        null, true, Tier.STANDARD),
                new CustomerDef("Phan Quốc Hưng",       "hung.phan@email.com",       "0901556677", "male",
                        LocalDate.of(1994, 7, 30), "12 Bà Triệu, Hà Nội",
                        null, false, Tier.STANDARD),
                new CustomerDef("Tô Minh Anh",          "anh.to@email.com",          "0901778899", "female",
                        LocalDate.of(1999, 5, 16), "34 Phạm Ngọc Thạch, Q.3, TP.HCM",
                        null, true, Tier.STANDARD),
                new CustomerDef("Cao Thị Diệu",         "dieu.cao@email.com",        "0901990011", "female",
                        LocalDate.of(1997, 3, 9),  "56 Trần Khắc Chân, Q.1, TP.HCM",
                        null, true, Tier.STANDARD)
        );

        List<User> result = new ArrayList<>();
        for (CustomerDef d : defs) {
            User existing = userRepository.findByEmail(d.email).orElse(null);
            if (existing != null) {
                result.add(existing);
                continue;
            }
            User u = User.builder()
                    .email(d.email)
                    .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .fullName(d.fullName)
                    .phone(d.phone)
                    .avatarUrl(d.avatarUrl)
                    .gender(d.gender)
                    .dateOfBirth(d.dob)
                    .address(d.address)
                    .nationality("Việt Nam")
                    .marketingOptIn(d.marketingOptIn)
                    .role(travelerRole)
                    .isActive(true)
                    .lastLoginAt(Instant.now().minus(rand(0, 30), ChronoUnit.DAYS))
                    .build();
            // Lưu tier vào adminNote tạm để cuối hàm assignBookings biết phân bổ.
            // (tier không được lưu DB; chỉ dùng cho phân bổ booking).
            u.setAdminNote(d.tier.name());
            result.add(userRepository.save(u));
        }
        log.info("Seeded {} customers", result.size());
        return result;
    }

    // ---------------- Sessions: assign guides ----------------

    private void assignGuidesToSessions(List<User> guides) {
        if (guides.isEmpty()) return;
        List<TourSession> sessions = tourSessionRepository.findAll();
        int idx = 0;
        int assigned = 0;
        for (TourSession s : sessions) {
            // Chỉ gán cho ~70% session để giữ tính đa dạng.
            if (s.getTourGuide() != null) continue;
            if (idx % 10 < 7) {
                s.setTourGuide(guides.get(idx % guides.size()));
                tourSessionRepository.save(s);
                assigned++;
            }
            idx++;
        }
        log.info("Assigned guides to {} sessions", assigned);
    }

    // ---------------- Bookings ----------------

    private void seedBookings(List<User> customers, List<Tour> tours) {
        Random rnd = new Random(42); // deterministic
        List<TourSession> sessions = tourSessionRepository.findAll();
        if (sessions.isEmpty()) {
            log.warn("No tour sessions. Skip bookings seed.");
            return;
        }

        // Mỗi tour có ít nhất 1 session; ta cũng thêm 1 session quá khứ cho 5 tour đầu để tạo
        // booking "completed".
        List<TourSession> pastSessions = createPastSessions(tours, 5);

        for (User cust : customers) {
            Tier tier = parseTier(cust.getAdminNote());
            // Bookings count theo tier
            int n = switch (tier) {
                case VIP -> 4;
                case GOLD -> 3;
                case SILVER -> 2;
                case STANDARD -> rnd.nextInt(2); // 0 or 1
            };

            for (int i = 0; i < n; i++) {
                boolean wantPast = (tier == Tier.VIP || tier == Tier.GOLD) && i < 2 && !pastSessions.isEmpty();
                TourSession s = wantPast
                        ? pastSessions.get(rnd.nextInt(pastSessions.size()))
                        : sessions.get(rnd.nextInt(sessions.size()));

                String status = pickStatus(rnd, tier, i, wantPast);
                int guests = 1 + rnd.nextInt(4);
                seedOneSyntheticBooking(cust, s, status, guests, rnd);
            }
        }

        // Cleanup: remove tier hint trong adminNote.
        for (User c : customers) {
            if (c.getAdminNote() != null && Tier.contains(c.getAdminNote())) {
                c.setAdminNote(null);
                userRepository.save(c);
            }
        }
    }

    /**
     * Một đơn demo đầy đủ (khách, thanh toán/refund tùy status, cập nhật chỗ session).
     */
    private void seedOneSyntheticBooking(User cust, TourSession s, String status, int guests, Random rnd) {
        BigDecimal price = s.getTour().getBasePrice() != null ? s.getTour().getBasePrice() : new BigDecimal("2000000");
        BigDecimal total = price.multiply(BigDecimal.valueOf(guests));

        Booking b = Booking.builder()
                .user(cust)
                .session(s)
                .totalAmount(total)
                .guestCount(guests)
                .status(status)
                .contactPhone(cust.getPhone())
                .pickupAddress(cust.getAddress())
                .guestNames(buildGuestNames(cust, guests))
                .emergencyContactName("Người thân của " + firstName(cust.getFullName()))
                .emergencyContactPhone("09" + (10000000 + rnd.nextInt(90000000)))
                .specialRequests(rnd.nextDouble() < 0.4
                        ? "Yêu cầu ghế cửa sổ, ăn chay cho 1 khách"
                        : null)
                .build();
        b = bookingRepository.save(b);

        attachGuests(b, cust, guests);
        attachPaymentsForStatus(b, status, total, rnd);
        maybeAttachRefund(b, status, rnd);

        if (!"cancelled".equals(status)) {
            s.setCurrentParticipants(Math.min(s.getMaxParticipants(),
                    (s.getCurrentParticipants() == null ? 0 : s.getCurrentParticipants()) + guests));
            tourSessionRepository.save(s);
        }
    }

    /**
     * UserSeeder tạo {@code traveler@example.com} nhưng {@link #seedBookings} chỉ gán đơn cho 15 email demo —
     * bổ sung 4 đơn (paid / pending / cancelled) để FE luôn có dữ liệu khi đăng nhập tài khoản traveler mặc định.
     */
    private void ensureDefaultTravelerHasDemoBookings() {
        User traveler = userRepository.findByEmail("traveler@example.com").orElse(null);
        if (traveler == null) {
            log.debug("traveler@example.com not found, skip default traveler bookings.");
            return;
        }
        if (bookingRepository.countByUser_Id(traveler.getId()) > 0) {
            log.debug("traveler@example.com already has booking(s), skip.");
            return;
        }
        List<TourSession> scheduled = tourSessionRepository.findAll().stream()
                .filter(s -> "scheduled".equalsIgnoreCase(s.getStatus()))
                .toList();
        if (scheduled.isEmpty()) {
            log.warn("No scheduled tour sessions — cannot seed traveler@example.com bookings.");
            return;
        }
        Random rnd = new Random(99);
        record TravelerPlan(String status, int guests) { }
        List<TravelerPlan> plans = List.of(
                new TravelerPlan("paid", 2),
                new TravelerPlan("paid", 1),
                new TravelerPlan("pending", 2),
                new TravelerPlan("cancelled", 1)
        );
        int i = 0;
        for (TravelerPlan p : plans) {
            TourSession s = scheduled.get(i % scheduled.size());
            seedOneSyntheticBooking(traveler, s, p.status(), p.guests(), rnd);
            i++;
        }
        log.info("Seeded {} demo booking(s) for traveler@example.com (password: Traveler@123)", plans.size());
    }

    /** Tạo session quá khứ (đã hoàn thành) cho N tour đầu để có booking 'completed'. */
    private List<TourSession> createPastSessions(List<Tour> tours, int n) {
        List<TourSession> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, tours.size()); i++) {
            Tour t = tours.get(i);
            int days = t.getDurationDays() == null ? 2 : t.getDurationDays();
            LocalDate end = LocalDate.now().minusDays(15 + i * 10);
            LocalDate start = end.minusDays(days);
            TourSession s = TourSession.builder()
                    .tour(t)
                    .startDate(start)
                    .endDate(end)
                    .maxParticipants(20)
                    .currentParticipants(0)
                    .status("completed")
                    .build();
            result.add(tourSessionRepository.save(s));
        }
        return result;
    }

    private String pickStatus(Random rnd, Tier tier, int bookingIdx, boolean isPast) {
        if (isPast) {
            // Bookings cho session quá khứ: phần lớn completed, có cancelled.
            return rnd.nextDouble() < 0.85 ? "completed" : "cancelled";
        }
        // Phân bổ status cho booking tương lai theo tier.
        double r = rnd.nextDouble();
        if (tier == Tier.VIP || tier == Tier.GOLD) {
            if (r < 0.50) return "paid";
            if (r < 0.80) return "confirmed";
            if (r < 0.92) return "pending";
            return "cancelled";
        }
        if (r < 0.35) return "paid";
        if (r < 0.60) return "pending";
        if (r < 0.85) return "confirmed";
        return "cancelled";
    }

    private void attachGuests(Booking b, User cust, int guests) {
        List<BookingGuest> gs = b.getBookingGuests();
        // Chỉ lưu khách kèm — người đặt đã có trên booking.user và roster LEAD; tránh trùng 1 slot.
        for (int i = 1; i < guests; i++) {
            gs.add(BookingGuest.builder()
                    .booking(b)
                    .fullName("Người đi cùng " + i + " - " + firstName(cust.getFullName()))
                    .idNumber("0790" + (100000000L + new Random(cust.getId().hashCode() + i).nextInt(900000000)))
                    .dateOfBirth(cust.getDateOfBirth() == null ? LocalDate.of(1990, 1, 1) : cust.getDateOfBirth().minusYears(2 * i))
                    .sortOrder(i - 1)
                    .build());
        }
        bookingRepository.save(b);
    }

    private void attachPaymentsForStatus(Booking b, String status, BigDecimal total, Random rnd) {
        switch (status) {
            case "paid", "completed", "confirmed" -> {
                // Đôi khi có 1 lần fail trước khi thành công (~10%) để minh hoạ success rate.
                if (rnd.nextDouble() < 0.10) {
                    String failProvider = pickProvider(rnd);
                    Payment failed = buildSeedPayment(b, failProvider, total.multiply(new BigDecimal("0.30")).setScale(0, java.math.RoundingMode.HALF_UP), rnd);
                    failed.setStatus("failed");
                    failed.setFailureReason("Cổng thanh toán trả về lỗi tạm thời (demo)");
                    paymentRepository.save(failed);
                }
                // Full payment paid
                String provider = pickProvider(rnd);
                Payment p = buildSeedPayment(b, provider, total, rnd);
                p.setStatus("paid");
                p.setPaidAt(Instant.now().minus(rand(0, 60), ChronoUnit.MINUTES));
                paymentRepository.save(p);
            }
            case "pending" -> {
                if (rnd.nextDouble() < 0.4) {
                    BigDecimal deposit = total.multiply(new BigDecimal(rnd.nextDouble() < 0.5 ? "0.30" : "0.50"))
                            .setScale(0, java.math.RoundingMode.HALF_UP);
                    String provider = pickProvider(rnd);
                    Payment p = buildSeedPayment(b, provider, deposit, rnd);
                    p.setStatus("paid");
                    p.setPaidAt(Instant.now().minus(rand(0, 120), ChronoUnit.MINUTES));
                    paymentRepository.save(p);
                }
            }
            case "cancelled" -> {
                if (rnd.nextDouble() < 0.5) {
                    String provider = pickProvider(rnd);
                    BigDecimal amount = total.multiply(new BigDecimal("0.50")).setScale(0, java.math.RoundingMode.HALF_UP);
                    Payment p = buildSeedPayment(b, provider, amount, rnd);
                    p.setStatus("paid");
                    p.setPaidAt(Instant.now().minus(rand(1, 30), ChronoUnit.DAYS));
                    paymentRepository.save(p);
                }
            }
            default -> { /* no payment */ }
        }
    }

    private Payment buildSeedPayment(Booking b, String provider, BigDecimal amount, Random rnd) {
        BigDecimal fee = computeFee(provider, amount);
        return Payment.builder()
                .booking(b)
                .provider(provider)
                .partnerCode("SEED-" + provider.toUpperCase().substring(0, Math.min(4, provider.length())))
                .orderId("SEED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .requestId("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .providerTransId("TX-" + (1000000 + rnd.nextInt(9000000)))
                .amount(amount)
                .feeAmount(fee)
                .currency("VND")
                .status("pending")
                .signature("seed-signature")
                .build();
    }

    private String pickProvider(Random rnd) {
        double r = rnd.nextDouble();
        if (r < 0.45) return "momo";
        if (r < 0.70) return "vnpay";
        if (r < 0.85) return "bank_transfer";
        if (r < 0.95) return "credit_card";
        return "manual";
    }

    private BigDecimal computeFee(String provider, BigDecimal amount) {
        if (amount == null || amount.signum() == 0) return BigDecimal.ZERO;
        BigDecimal rate = switch (provider == null ? "" : provider) {
            case "momo" -> new BigDecimal("0.015");        // 1.5%
            case "vnpay" -> new BigDecimal("0.018");       // 1.8%
            case "credit_card" -> new BigDecimal("0.025"); // 2.5%
            case "bank_transfer" -> new BigDecimal("0.005"); // 0.5%
            default -> BigDecimal.ZERO; // manual = 0
        };
        return amount.multiply(rate).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private void maybeAttachRefund(Booking b, String status, Random rnd) {
        // 15% bookings paid/confirmed/cancelled-with-payment có 1 refund pending.
        if (!("paid".equals(status) || "confirmed".equals(status) || "cancelled".equals(status))) return;
        if (rnd.nextDouble() > 0.15) return;

        BigDecimal refundAmt = b.getTotalAmount().multiply(new BigDecimal("0.30")).setScale(0, java.math.RoundingMode.HALF_UP);
        Refund r = Refund.builder()
                .booking(b)
                .amount(refundAmt)
                .reason("KH yêu cầu hoàn cọc do lịch trình thay đổi")
                .status("pending")
                .build();
        refundRepository.save(r);
    }

    // ---------------- Helpers ----------------

    private static String employeeCodeFromId(UUID id) {
        return "EMP-" + id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static int rand(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "khách";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private static String buildGuestNames(User cust, int guests) {
        StringBuilder sb = new StringBuilder(cust.getFullName());
        for (int i = 1; i < guests; i++) {
            sb.append(", Người đi cùng ").append(i).append(" - ").append(firstName(cust.getFullName()));
        }
        return sb.toString();
    }

    private static Tier parseTier(String hint) {
        if (hint == null) return Tier.STANDARD;
        try { return Tier.valueOf(hint); } catch (Exception e) { return Tier.STANDARD; }
    }

    private enum Tier {
        VIP, GOLD, SILVER, STANDARD;
        static boolean contains(String s) {
            for (Tier t : values()) if (t.name().equals(s)) return true;
            return false;
        }
    }

    private record GuideDef(String fullName, String email, String phone, String gender,
                            LocalDate dob, String address, String avatarUrl) { }

    private record CustomerDef(String fullName, String email, String phone, String gender,
                               LocalDate dob, String address, String avatarUrl,
                               boolean marketingOptIn, Tier tier) { }
}

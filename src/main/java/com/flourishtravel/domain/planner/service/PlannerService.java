package com.flourishtravel.domain.planner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.domain.destination.entity.Destination;
import com.flourishtravel.domain.destination.entity.DestinationAttraction;
import com.flourishtravel.domain.destination.entity.DestinationHighlightSpot;
import com.flourishtravel.domain.destination.entity.DestinationMapPoi;
import com.flourishtravel.domain.destination.repository.DestinationRepository;
import com.flourishtravel.domain.planner.dto.*;
import com.flourishtravel.domain.planner.entity.TripPlan;
import com.flourishtravel.domain.planner.repository.TripPlanRepository;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlannerService {

    private static final List<String> DEFAULT_TIMES = List.of(
            "08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00");

    private final DestinationRepository destinationRepository;
    private final TripPlanRepository tripPlanRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PlannerGenerateResponse generate(PlannerGenerateRequest request) {
        validateRequest(request);
        List<Destination> dests = resolveDestinations(request.getDestinations());
        int days = computeDays(request.getStartDate(), request.getEndDate());
        int nights = Math.max(0, days - 1);
        int adults = request.getAdults() != null ? request.getAdults() : 2;
        int children = request.getChildren() != null ? request.getChildren() : 0;
        int guests = adults + children;

        List<PlannerActivityDto> pool = buildActivityPool(dests, request);
        List<PlannerDayDto> dayPlans = distributeAcrossDays(pool, dests, days, request);
        PlannerBudgetDto budget = calculateBudget(request, days, nights, guests, dests);
        PlannerSuggestionDto suggestion = buildSuggestion(dests, request);

        String destNames = dests.stream().map(Destination::getName).collect(Collectors.joining(", "));
        String summary = days + " ngày " + nights + " đêm · " + destNames;

        List<String> steps = List.of(
                "Phân tích ngân sách",
                "Kiểm tra thời tiết",
                "Tối ưu điểm tham quan",
                "Đặt chỗ nhà hàng");

        return PlannerGenerateResponse.builder()
                .sessionId(UUID.randomUUID())
                .tripSummary(summary)
                .daysCount(days)
                .nightsCount(nights)
                .days(dayPlans)
                .activityPool(remainingPool(pool, dayPlans))
                .budget(budget)
                .optimization(PlannerOptimizationDto.builder()
                        .status("completed")
                        .progressPercent(100)
                        .steps(steps)
                        .stepsCompleted(List.of(true, true, true, true))
                        .build())
                .suggestion(suggestion)
                .build();
    }

    @Transactional(readOnly = true)
    public PlannerSuggestionDto getSuggestion(String city, LocalDate date) {
        String slug = city != null ? city.toLowerCase().replace(" ", "-") : "bangkok";
        if ("bangkok".equals(slug) || city != null && city.toLowerCase().contains("bangkok")) {
            return PlannerSuggestionDto.builder()
                    .type("weather")
                    .message("Bangkok dự báo sẽ có mưa rào vào chiều tối nay. Bạn có muốn thay đổi lịch trình tham quan ngoài trời?")
                    .currentActivityTitle("Safari World")
                    .suggestedActivityTitle("Sea Life Bangkok Ocean World")
                    .suggestedActivityDescription("Trải nghiệm trong nhà, phù hợp khi trời mưa.")
                    .suggestedImageUrl("https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=400&q=80")
                    .build();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public PlannerBudgetDto calculateBudgetFromDays(PlannerGenerateRequest request, List<PlannerDayDto> days) {
        int dayCount = days != null ? days.size() : 1;
        int nights = Math.max(0, dayCount - 1);
        int guests = (request.getAdults() != null ? request.getAdults() : 2)
                + (request.getChildren() != null ? request.getChildren() : 0);
        List<Destination> dests = resolveDestinations(request.getDestinations());
        return calculateBudget(request, dayCount, nights, guests, dests);
    }

    @Transactional
    public SavedTripPlanDto save(PlannerSaveRequest body, UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để lưu lịch trình");
        }
        try {
            TripPlan plan = TripPlan.builder()
                    .userId(principal.getId())
                    .title(body.getTitle() != null && !body.getTitle().isBlank()
                            ? body.getTitle()
                            : "Lịch trình Flourish")
                    .tripSummary(buildSummary(body.getRequest()))
                    .requestJson(objectMapper.writeValueAsString(body.getRequest()))
                    .itineraryJson(objectMapper.writeValueAsString(body.getDays()))
                    .budgetJson(objectMapper.writeValueAsString(body.getBudget()))
                    .build();
            plan = tripPlanRepository.save(plan);
            return SavedTripPlanDto.builder()
                    .id(plan.getId())
                    .title(plan.getTitle())
                    .createdAt(plan.getCreatedAt())
                    .tripSummary(plan.getTripSummary())
                    .build();
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu lịch trình không hợp lệ");
        }
    }

    @Transactional(readOnly = true)
    public List<SavedTripPlanDto> listSaved(UserPrincipal principal) {
        if (principal == null) {
            return List.of();
        }
        return tripPlanRepository.findByUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(p -> SavedTripPlanDto.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .createdAt(p.getCreatedAt())
                        .tripSummary(p.getTripSummary())
                        .build())
                .toList();
    }

    public Map<String, String> exportPdf(UUID planId, UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        TripPlan plan = tripPlanRepository.findById(planId)
                .filter(p -> p.getUserId().equals(principal.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch trình"));
        return Map.of(
                "message", "PDF đã được tạo (demo)",
                "planId", plan.getId().toString(),
                "title", plan.getTitle(),
                "downloadHint", "Tích hợp iText/Flying Saucer trong phiên bản production");
    }

    public Map<String, String> syncCalendar(UUID planId, UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        tripPlanRepository.findById(planId)
                .filter(p -> p.getUserId().equals(principal.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch trình"));
        return Map.of(
                "message", "Đã gửi yêu cầu đồng bộ Google Calendar (demo)",
                "status", "pending_oauth");
    }

    private void validateRequest(PlannerGenerateRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ngày khởi hành và kết thúc");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày khởi hành");
        }
        if (request.getDestinations() == null || request.getDestinations().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn ít nhất một điểm đến");
        }
    }

    private List<Destination> resolveDestinations(List<String> slugs) {
        List<Destination> result = new ArrayList<>();
        for (String raw : slugs) {
            String slug = raw.trim().toLowerCase();
            destinationRepository.findBySlugAndPublishedTrue(slug).ifPresent(result::add);
        }
        if (result.isEmpty()) {
            destinationRepository.findBySlugAndPublishedTrue("bangkok").ifPresent(result::add);
        }
        return result;
    }

    private int computeDays(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    private List<PlannerActivityDto> buildActivityPool(List<Destination> dests, PlannerGenerateRequest request) {
        List<PlannerActivityDto> pool = new ArrayList<>();
        boolean relaxed = request.getExperienceLevel() == null || request.getExperienceLevel() < 40;

        for (Destination d : dests) {
            pool.add(activity("Đến " + d.getName(), "Check-in và làm quen khu vực", d.getHeroImageUrl(),
                    "arrival", d.getName(), d.getLatitude(), d.getLongitude(), false, null));

            if (!relaxed) {
                pool.add(activity("Ăn sáng tại khách sạn", "Nạp năng lượng cho ngày mới",
                        d.getHeroImageUrl(), "meal", d.getName(), d.getLatitude(), d.getLongitude(), false, null));
            }

            int floraIdx = 0;
            for (DestinationAttraction a : d.getAttractions()) {
                boolean flora = floraIdx++ == 0;
                pool.add(activity(a.getName(), a.getDescription(), a.getImageUrl(),
                        "attraction", d.getName(), a.getLatitude(), a.getLongitude(), flora, a.getTicketPriceLabel()));
            }
            for (DestinationHighlightSpot h : d.getHighlightSpots()) {
                boolean exists = pool.stream().anyMatch(p -> p.getTitle().equals(h.getName()));
                if (!exists) {
                    DestinationMapPoi poi = d.getMapPois().stream()
                            .filter(p -> h.getName().equalsIgnoreCase(p.getName())
                                    || p.getName().contains(h.getName()))
                            .findFirst().orElse(null);
                    pool.add(activity(h.getName(), "Điểm nổi bật tại " + d.getName(), d.getHeroImageUrl(),
                            "attraction", d.getName(),
                            poi != null ? poi.getLatitude() : d.getLatitude(),
                            poi != null ? poi.getLongitude() : d.getLongitude(),
                            false, poi != null ? poi.getPriceLabel() : null));
                }
            }

            for (DestinationMapPoi p : d.getMapPois()) {
                if ("restaurant".equals(p.getCategory())) {
                    pool.add(activity("Ăn trưa tại " + p.getName(), "Ẩm thực địa phương",
                            p.getImageUrl() != null ? p.getImageUrl() : d.getHeroImageUrl(),
                            "restaurant", d.getName(), p.getLatitude(), p.getLongitude(), false, p.getPriceLabel()));
                }
            }

            if (request.getStyles() != null && request.getStyles().stream().anyMatch(s -> s.contains("food"))) {
                pool.add(activity("Khám phá ẩm thực đường phố", "Pad Thai, Tom Yum, Mango Sticky Rice",
                        d.getHeroImageUrl(), "food", d.getName(), d.getLatitude(), d.getLongitude(), true, null));
            }

            pool.add(activity("Nghỉ ngơi tại khách sạn", "Kết thúc ngày tham quan",
                    d.getHeroImageUrl(), "rest", d.getName(), d.getLatitude(), d.getLongitude(), false, null));
        }

        if ("bangkok".equals(dests.get(0).getSlug()) || dests.stream().anyMatch(d -> "bangkok".equals(d.getSlug()))) {
            pool.add(activity("Safari World", "Công viên động vật ngoài trời",
                    "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?w=400&q=80",
                    "attraction", "Bangkok", 13.85, 100.70, false, "1.200.000đ/người"));
            pool.add(activity("Chao Phraya Cruise", "Du thuyền sông Chao Phraya buổi tối",
                    "https://images.unsplash.com/photo-1563492065-608b2d5b4f52?w=400&q=80",
                    "cruise", "Bangkok", 13.72, 100.51, false, "800.000đ/người"));
            pool.add(activity("Ayutthaya Tour", "Di sản UNESCO trong ngày",
                    "https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=400&q=80",
                    "tour", "Ayutthaya", 14.35, 100.57, false, "1.500.000đ/người"));
        }

        return pool;
    }

    private PlannerActivityDto activity(String title, String desc, String image, String category,
            String location, Double lat, Double lng, boolean flora, String price) {
        return PlannerActivityDto.builder()
                .id(UUID.randomUUID())
                .time("")
                .title(title)
                .description(desc)
                .imageUrl(image)
                .category(category)
                .locationName(location)
                .latitude(lat)
                .longitude(lng)
                .floraRecommended(flora)
                .priceLabel(price)
                .build();
    }

    private List<PlannerDayDto> distributeAcrossDays(List<PlannerActivityDto> pool,
            List<Destination> dests, int days, PlannerGenerateRequest request) {
        List<PlannerDayDto> result = new ArrayList<>();
        List<PlannerActivityDto> queue = new ArrayList<>(pool);
        int destIdx = 0;

        for (int d = 1; d <= days; d++) {
            Destination dest = dests.get(destIdx % dests.size());
            destIdx++;
            int perDay = Math.max(3, Math.min(6, (int) Math.ceil((double) queue.size() / (days - d + 1))));
            List<PlannerActivityDto> dayActs = new ArrayList<>();
            for (int i = 0; i < perDay && !queue.isEmpty(); i++) {
                PlannerActivityDto act = queue.remove(0);
                act.setTime(DEFAULT_TIMES.get(Math.min(i, DEFAULT_TIMES.size() - 1)));
                dayActs.add(act);
            }
            result.add(PlannerDayDto.builder()
                    .dayNumber(d)
                    .label("Ngày " + d)
                    .destinationName(dest.getName())
                    .activities(dayActs)
                    .build());
        }
        return result;
    }

    private List<PlannerActivityDto> remainingPool(List<PlannerActivityDto> pool, List<PlannerDayDto> days) {
        Set<UUID> used = days.stream()
                .flatMap(d -> d.getActivities().stream())
                .map(PlannerActivityDto::getId)
                .collect(Collectors.toSet());
        return pool.stream().filter(a -> !used.contains(a.getId())).toList();
    }

    private PlannerBudgetDto calculateBudget(PlannerGenerateRequest request, int days, int nights,
            int guests, List<Destination> dests) {
        long budget = request.getBudgetVnd() != null ? request.getBudgetVnd() : 15_000_000L;
        if (Boolean.TRUE.equals(request.getBudgetPerPerson())) {
            budget = budget * guests;
        }

        long flight = Boolean.TRUE.equals(request.getIncludeFlight()) ? 4_500_000L * guests : 0L;
        int stars = request.getHotelStars() != null ? request.getHotelStars() : 4;
        long perNight = stars >= 5 ? 2_600_000L : stars == 3 ? 800_000L : 1_200_000L;
        long hotel = perNight * nights;
        long food = 380_000L * days * guests;
        long sightseeing = 1_500_000L * days;
        long transport = 1_000_000L * days;
        if (request.getTransport() != null) {
            if (request.getTransport().contains("car_rental")) transport += 500_000L * days;
        }

        List<PlannerBudgetLineDto> lines = new ArrayList<>();
        if (flight > 0) lines.add(line("flight", "Vé máy bay (khứ hồi)", flight));
        lines.add(line("hotel", "Khách sạn (" + nights + " đêm)", hotel));
        lines.add(line("food", "Ăn uống & chi tiêu", food));
        lines.add(line("sightseeing", "Vé tham quan", sightseeing));
        lines.add(line("transport", "Di chuyển", transport));

        long total = lines.stream().mapToLong(PlannerBudgetLineDto::getAmountVnd).sum();

        return PlannerBudgetDto.builder()
                .lines(lines)
                .totalVnd(total)
                .budgetVnd(budget)
                .withinBudget(total <= budget)
                .build();
    }

    private PlannerBudgetLineDto line(String cat, String label, long amount) {
        return PlannerBudgetLineDto.builder().category(cat).label(label).amountVnd(amount).build();
    }

    private PlannerSuggestionDto buildSuggestion(List<Destination> dests, PlannerGenerateRequest request) {
        boolean bangkok = dests.stream().anyMatch(d -> "bangkok".equals(d.getSlug()));
        if (bangkok) {
            return getSuggestion("bangkok", request.getStartDate());
        }
        return null;
    }

    private String buildSummary(PlannerGenerateRequest request) {
        if (request == null || request.getStartDate() == null) return "";
        int days = computeDays(request.getStartDate(), request.getEndDate());
        return days + " ngày " + Math.max(0, days - 1) + " đêm";
    }
}

package com.flourishtravel.config;

import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourLocation;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.entity.TourVideo;
import com.flourishtravel.domain.tour.repository.CategoryRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Seed dữ liệu demo: category + tour + session + ảnh + itinerary + location + video.
 * - Khi chưa có tour: tạo đủ 16 tour đầy đủ dữ liệu.
 * - Khi đã có tour: bổ sung itinerary/location/video cho tour cũ thiếu; thêm tour mới (slug chưa có) cho đủ 16.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TourSeeder {

    private final CategoryRepository categoryRepository;
    private final TourRepository tourRepository;

    private static final String PLACEHOLDER_IMAGE = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400";
    private static final String PLACEHOLDER_IMAGE_2 = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=400";
    private static final String PLACEHOLDER_VIDEO = "https://www.youtube.com/embed/dQw4w9WgXcQ";
    private static final String PLACEHOLDER_THUMB = "https://img.youtube.com/vi/dQw4w9WgXcQ/mqdefault.jpg";

    @EventListener(ApplicationReadyEvent.class)
    @Order(4)
    @Transactional
    public void seedTours() {
        Category catBeach = categoryRepository.findBySlug("tour-bien").orElseGet(() -> {
            Category c = Category.builder()
                    .name("Tour biển")
                    .slug("tour-bien")
                    .description("Tour nghỉ dưỡng biển")
                    .sortOrder(1)
                    .build();
            return categoryRepository.save(c);
        });
        Category catExplore = categoryRepository.findBySlug("kham-pha").orElseGet(() -> {
            Category c = Category.builder()
                    .name("Khám phá")
                    .slug("kham-pha")
                    .description("Tour khám phá văn hóa, thiên nhiên")
                    .sortOrder(2)
                    .build();
            return categoryRepository.save(c);
        });

        List<Tour> tourDefs = List.of(
                tour("Tour biển Đà Nẵng 3 ngày 2 đêm", "tour-bien-da-nang-3n", "Trải nghiệm biển Mỹ Khê, Bà Nà, Hội An.", new BigDecimal("3990000"), 3, 2, catBeach),
                tour("Tour Phan Thiết – Mũi Né 2 ngày", "tour-phan-thiet-mui-ne-2n", "Biển Mũi Né, đồi cát bay, làng chài.", new BigDecimal("2490000"), 2, 1, catBeach),
                tour("Tour Đà Lạt 3 ngày 2 đêm", "tour-da-lat-3n", "Thành phố ngàn hoa, thác Datanla, đồi chè.", new BigDecimal("3590000"), 3, 2, catExplore),
                tour("Tour Nha Trang 4 ngày 3 đêm", "tour-nha-trang-4n", "Vinpearl, vịnh Nha Trang, tắm bùn.", new BigDecimal("5990000"), 4, 3, catBeach),
                tour("Tour Hạ Long 2 ngày 1 đêm", "tour-ha-long-2n", "Du thuyền vịnh Hạ Long, hang Sửng Sốt.", new BigDecimal("1890000"), 2, 1, catExplore),
                tour("Tour Phú Quốc 3 ngày 2 đêm", "tour-phu-quoc-3n", "Đảo ngọc, bãi Sao, safari, làng chài.", new BigDecimal("4490000"), 3, 2, catBeach),
                tour("Tour Cần Thơ – Châu Đốc 2 ngày", "tour-can-tho-chau-doc-2n", "Chợ nổi, miếu Bà Chúa Xứ, núi Sam.", new BigDecimal("2190000"), 2, 1, catExplore),
                tour("Tour Sapa 2 ngày 1 đêm", "tour-sapa-2n", "Fansipan, bản Cát Cát, thung lũng Mường Hoa.", new BigDecimal("2790000"), 2, 1, catExplore),
                tour("Tour Hội An – Cù Lao Chàm 1 ngày", "tour-hoi-an-cu-lao-cham-1n", "Phố cổ Hội An, lặn ngắm san hô Cù Lao Chàm.", new BigDecimal("890000"), 1, 0, catBeach),
                tour("Tour Vũng Tàu 2 ngày 1 đêm", "tour-vung-tau-2n", "Bãi Sau, Bãi Trước, tượng Chúa Kitô.", new BigDecimal("1590000"), 2, 1, catBeach),
                tour("Tour Ninh Bình – Tam Cốc 2 ngày", "tour-ninh-binh-tam-coc-2n", "Tam Cốc, Bái Đính, Tràng An.", new BigDecimal("1990000"), 2, 1, catExplore),
                tour("Tour Huế 2 ngày 1 đêm", "tour-hue-2n", "Đại nội, chùa Thiên Mụ, lăng Tự Đức.", new BigDecimal("1890000"), 2, 1, catExplore),
                tour("Tour Mũi Né 3 ngày 2 đêm", "tour-mui-ne-3n", "Đồi cát, làng chài, biển Mũi Né.", new BigDecimal("3290000"), 3, 2, catBeach),
                tour("Tour Đà Nẵng – Hội An 2 ngày", "tour-da-nang-hoi-an-2n", "Cầu Rồng, Bà Nà, phố cổ Hội An.", new BigDecimal("2690000"), 2, 1, catExplore),
                tour("Tour Côn Đảo 3 ngày 2 đêm", "tour-con-dao-3n", "Biển Côn Đảo, di tích lịch sử, rùa đẻ trứng.", new BigDecimal("6990000"), 3, 2, catBeach),
                tour("Tour Quy Nhơn 2 ngày 1 đêm", "tour-quy-nhon-2n", "Bãi Xép, Kỳ Co, Eo Gió.", new BigDecimal("2290000"), 2, 1, catBeach)
        );

        LocalDate start = LocalDate.now().plusDays(7);

        if (tourRepository.count() == 0) {
            for (Tour t : tourDefs) {
                saveTourWithDetails(tourRepository.save(t), start);
            }
            log.info("Seeded {} demo tours with sessions, images, itineraries, locations and videos", tourDefs.size());
        } else {
            for (Tour existing : tourRepository.findAll()) {
                backfillTour(existing, start);
            }
            int added = 0;
            for (Tour t : tourDefs) {
                if (tourRepository.findBySlug(t.getSlug()).isPresent()) continue;
                Tour saved = tourRepository.save(t);
                saveTourWithDetails(saved, start);
                added++;
            }
            log.info("Backfilled existing tours and added {} missing tours (total target 16)", added);
        }
    }

    private void saveTourWithDetails(Tour saved, LocalDate start) {
        int days = saved.getDurationDays() != null ? saved.getDurationDays() : 2;

        if (saved.getSessions().isEmpty()) {
            saved.getSessions().add(TourSession.builder()
                    .tour(saved)
                    .startDate(start)
                    .endDate(start.plusDays(days))
                    .maxParticipants(20)
                    .currentParticipants(0)
                    .status("scheduled")
                    .build());
        }
        if (saved.getImages().size() < 2) {
            if (saved.getImages().isEmpty()) {
                saved.getImages().add(TourImage.builder().tour(saved).imageUrl(PLACEHOLDER_IMAGE).caption(saved.getTitle()).sortOrder(0).build());
            }
            saved.getImages().add(TourImage.builder().tour(saved).imageUrl(PLACEHOLDER_IMAGE_2).caption(saved.getTitle() + " - trải nghiệm").sortOrder(1).build());
        }
        if (saved.getItineraries().isEmpty()) {
            for (int d = 1; d <= days; d++) {
                saved.getItineraries().add(TourItinerary.builder()
                        .tour(saved)
                        .dayNumber(d)
                        .title("Ngày " + d + ": " + (d == 1 ? "Khởi hành & điểm đến chính" : d == days ? "Trải nghiệm cuối & về" : "Khám phá"))
                        .description("Lịch trình chi tiết ngày " + d + " của tour. Điểm tham quan, ăn uống, nghỉ ngơi theo chương trình.")
                        .build());
            }
        }
        if (saved.getLocations().isEmpty()) {
            List<String> locNames = getLocationNamesForSlug(saved.getSlug());
            for (int i = 0; i < locNames.size(); i++) {
                String name = locNames.get(i);
                saved.getLocations().add(TourLocation.builder()
                        .tour(saved)
                        .locationName(name)
                        .latitude(locLat(name))
                        .longitude(locLng(name))
                        .visitOrder(i + 1)
                        .dayNumber(Math.min(i + 1, days))
                        .build());
            }
        }
        if (saved.getVideos().isEmpty()) {
            saved.getVideos().add(TourVideo.builder()
                    .tour(saved)
                    .videoUrl(PLACEHOLDER_VIDEO)
                    .thumbnailUrl(PLACEHOLDER_THUMB)
                    .title("Video giới thiệu " + saved.getTitle())
                    .durationSeconds(120)
                    .sortOrder(0)
                    .build());
        }
        tourRepository.save(saved);
    }

    private void backfillTour(Tour saved, LocalDate start) {
        saveTourWithDetails(saved, start);
    }

    private static Tour tour(String title, String slug, String desc, BigDecimal price, int days, int nights, Category category) {
        Tour t = new Tour();
        t.setTitle(title);
        t.setSlug(slug);
        t.setDescription(desc);
        t.setBasePrice(price);
        t.setDurationDays(days);
        t.setDurationNights(nights);
        t.setCategory(category);
        t.setSessions(new java.util.ArrayList<>());
        t.setImages(new java.util.ArrayList<>());
        t.setItineraries(new java.util.ArrayList<>());
        t.setLocations(new java.util.ArrayList<>());
        t.setVideos(new java.util.ArrayList<>());
        return t;
    }

    private static final Map<String, List<String>> SLUG_LOCATIONS = Map.ofEntries(
            Map.entry("tour-bien-da-nang-3n", List.of("Bãi biển Mỹ Khê", "Bà Nà Hills", "Phố cổ Hội An")),
            Map.entry("tour-phan-thiet-mui-ne-2n", List.of("Đồi cát bay", "Làng chài Mũi Né", "Bãi biển Mũi Né")),
            Map.entry("tour-da-lat-3n", List.of("Thác Datanla", "Đồi chè Cầu Đất", "Hồ Xuân Hương", "Chợ Đà Lạt")),
            Map.entry("tour-nha-trang-4n", List.of("Vinpearl Nha Trang", "Vịnh Nha Trang", "Tháp Bà Ponagar", "Hòn Chồng")),
            Map.entry("tour-ha-long-2n", List.of("Vịnh Hạ Long", "Hang Sửng Sốt", "Đảo Titop")),
            Map.entry("tour-phu-quoc-3n", List.of("Bãi Sao", "Vinpearl Safari", "Làng chài Hàm Ninh", "Dinh Cậu")),
            Map.entry("tour-can-tho-chau-doc-2n", List.of("Chợ nổi Cái Răng", "Miếu Bà Chúa Xứ", "Núi Sam")),
            Map.entry("tour-sapa-2n", List.of("Fansipan", "Bản Cát Cát", "Thung lũng Mường Hoa")),
            Map.entry("tour-hoi-an-cu-lao-cham-1n", List.of("Phố cổ Hội An", "Cù Lao Chàm")),
            Map.entry("tour-vung-tau-2n", List.of("Bãi Sau", "Tượng Chúa Kitô", "Bạch Dinh")),
            Map.entry("tour-ninh-binh-tam-coc-2n", List.of("Tam Cốc", "Chùa Bái Đính", "Tràng An")),
            Map.entry("tour-hue-2n", List.of("Đại nội Huế", "Chùa Thiên Mụ", "Lăng Tự Đức")),
            Map.entry("tour-mui-ne-3n", List.of("Đồi cát vàng", "Làng chài", "Bãi biển Mũi Né", "Suối Tiên")),
            Map.entry("tour-da-nang-hoi-an-2n", List.of("Cầu Rồng Đà Nẵng", "Bà Nà Hills", "Phố cổ Hội An")),
            Map.entry("tour-con-dao-3n", List.of("Bãi Ông Đụng", "Nhà tù Côn Đảo", "Bãi Đầm Trầu")),
            Map.entry("tour-quy-nhon-2n", List.of("Bãi Xép", "Kỳ Co", "Eo Gió"))
    );

    private static List<String> getLocationNamesForSlug(String slug) {
        return SLUG_LOCATIONS.getOrDefault(slug, List.of("Điểm tham quan 1", "Điểm tham quan 2"));
    }

    private static BigDecimal locLat(String name) {
        if (name.contains("Mỹ Khê") || name.contains("Bà Nà") || name.contains("Đà Nẵng")) return new BigDecimal("16.054407");
        if (name.contains("Hội An") || name.contains("Cù Lao")) return new BigDecimal("15.880058");
        if (name.contains("Mũi Né") || name.contains("Phan Thiết")) return new BigDecimal("10.928888");
        if (name.contains("Đà Lạt")) return new BigDecimal("11.940419");
        if (name.contains("Nha Trang")) return new BigDecimal("12.238791");
        if (name.contains("Hạ Long")) return new BigDecimal("20.910051");
        if (name.contains("Phú Quốc")) return new BigDecimal("10.227025");
        if (name.contains("Cần Thơ") || name.contains("Cái Răng")) return new BigDecimal("10.045162");
        if (name.contains("Châu Đốc") || name.contains("Núi Sam")) return new BigDecimal("10.704987");
        if (name.contains("Sapa") || name.contains("Fansipan")) return new BigDecimal("22.336381");
        if (name.contains("Vũng Tàu")) return new BigDecimal("10.411098");
        if (name.contains("Ninh Bình") || name.contains("Tam Cốc")) return new BigDecimal("20.214803");
        if (name.contains("Huế")) return new BigDecimal("16.463712");
        if (name.contains("Côn Đảo")) return new BigDecimal("8.682864");
        if (name.contains("Quy Nhơn")) return new BigDecimal("13.769588");
        return new BigDecimal("21.028511");
    }

    private static BigDecimal locLng(String name) {
        if (name.contains("Mỹ Khê") || name.contains("Bà Nà") || name.contains("Đà Nẵng")) return new BigDecimal("108.202164");
        if (name.contains("Hội An") || name.contains("Cù Lao")) return new BigDecimal("108.338047");
        if (name.contains("Mũi Né") || name.contains("Phan Thiết")) return new BigDecimal("108.102083");
        if (name.contains("Đà Lạt")) return new BigDecimal("108.458313");
        if (name.contains("Nha Trang")) return new BigDecimal("109.196749");
        if (name.contains("Hạ Long")) return new BigDecimal("107.183902");
        if (name.contains("Phú Quốc")) return new BigDecimal("103.967483");
        if (name.contains("Cần Thơ") || name.contains("Cái Răng")) return new BigDecimal("105.746857");
        if (name.contains("Châu Đốc") || name.contains("Núi Sam")) return new BigDecimal("105.118331");
        if (name.contains("Sapa") || name.contains("Fansipan")) return new BigDecimal("103.843611");
        if (name.contains("Vũng Tàu")) return new BigDecimal("107.084251");
        if (name.contains("Ninh Bình") || name.contains("Tam Cốc")) return new BigDecimal("105.921379");
        if (name.contains("Huế")) return new BigDecimal("107.590866");
        if (name.contains("Côn Đảo")) return new BigDecimal("106.608503");
        if (name.contains("Quy Nhơn")) return new BigDecimal("109.223469");
        return new BigDecimal("105.854444");
    }
}

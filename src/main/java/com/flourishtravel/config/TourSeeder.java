package com.flourishtravel.config;

import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourSession;
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

/**
 * Seed dữ liệu demo: category + tour + session + ảnh để chatbot và API tour có data.
 * Chỉ chạy khi chưa có tour nào trong DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TourSeeder {

    private final CategoryRepository categoryRepository;
    private final TourRepository tourRepository;

    private static final String PLACEHOLDER_IMAGE = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400";

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    @Transactional
    public void seedTours() {
        if (tourRepository.count() > 0) {
            log.debug("Tours already exist, skip seed");
            return;
        }

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

        List<Tour> tours = List.of(
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
        for (Tour t : tours) {
            Tour saved = tourRepository.save(t);
            TourSession session = TourSession.builder()
                    .tour(saved)
                    .startDate(start)
                    .endDate(start.plusDays(saved.getDurationDays() != null ? saved.getDurationDays() : 2))
                    .maxParticipants(20)
                    .currentParticipants(0)
                    .status("scheduled")
                    .build();
            saved.getSessions().add(session);
            TourImage img = TourImage.builder()
                    .tour(saved)
                    .imageUrl(PLACEHOLDER_IMAGE)
                    .caption(saved.getTitle())
                    .sortOrder(0)
                    .build();
            saved.getImages().add(img);
            tourRepository.save(saved);
        }

        log.info("Seeded {} demo tours with sessions and images", tours.size());
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
        return t;
    }
}

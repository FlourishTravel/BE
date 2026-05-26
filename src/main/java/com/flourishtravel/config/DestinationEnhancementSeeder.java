package com.flourishtravel.config;

import com.flourishtravel.domain.destination.entity.Destination;
import com.flourishtravel.domain.destination.entity.DestinationMapPoi;
import com.flourishtravel.domain.destination.entity.ThaiFestival;
import com.flourishtravel.domain.destination.repository.DestinationRepository;
import com.flourishtravel.domain.destination.repository.ThaiFestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bổ sung dữ liệu cho DB đã seed trước khi có tier / video / festival detail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DestinationEnhancementSeeder {

    private static final String YT_BANGKOK = "https://www.youtube.com/watch?v=7XNMzqNqDuE";
    private static final String YT_PHUKET = "https://www.youtube.com/watch?v=Kp0eYw85MfY";
    private static final String YT_LOY = "https://www.youtube.com/watch?v=0fbiqvK2s8k";
    private static final String YT_SONGKRAN = "https://www.youtube.com/watch?v=dxp9w9Ggehc";

    private final DestinationRepository destinationRepository;
    private final ThaiFestivalRepository festivalRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(5)
    @Transactional
    public void enhance() {
        enhanceFestivals();
        destinationRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getPublished()))
                .forEach(this::enhanceDestination);
    }

    private void enhanceFestivals() {
        patchFestival("loy-krathong", "chiang-mai", YT_LOY,
                "Loy Krathong diễn ra vào đêm trăng tròn tháng 12 âm lịch. Du khách thả krathong xuống sông để cầu may.",
                "Đặt khách sạn sớm — giá tăng 30–50% ở Chiang Mai\nMặc trang phục thoải mái gần đèn nến\nTham gia thả krathong tại bờ sông Chao Phraya");
        patchFestival("songkran", "bangkok", YT_SONGKRAN,
                "Songkran là Tết cổ truyền Thái Lan (13–15/4). Bangkok và Chiang Mai có các điểm té nước công cộng.",
                "Bọc điện thoại trong túi chống nước\nKhông té nước vào người già hoặc tu sĩ\nKhao San Road sôi động nhất ở Bangkok");
    }

    private void patchFestival(String slug, String relatedDest, String video, String longDesc, String tips) {
        festivalRepository.findBySlugAndPublishedTrue(slug).ifPresent(f -> {
            boolean changed = false;
            if (f.getLongDescription() == null) {
                f.setLongDescription(longDesc);
                changed = true;
            }
            if (f.getRelatedDestinationSlug() == null) {
                f.setRelatedDestinationSlug(relatedDest);
                changed = true;
            }
            if (f.getVideoUrl() == null) {
                f.setVideoUrl(video);
                changed = true;
            }
            if (f.getTips() == null) {
                f.setTips(tips);
                changed = true;
            }
            if (changed) {
                festivalRepository.save(f);
                log.info("Enhanced festival {}", slug);
            }
        });
    }

    private void enhanceDestination(Destination d) {
        boolean changed = false;
        if (d.getVideoUrl() == null) {
            String video = switch (d.getSlug()) {
                case "bangkok" -> YT_BANGKOK;
                case "phuket" -> YT_PHUKET;
                default -> null;
            };
            if (video != null) {
                d.setVideoUrl(video);
                changed = true;
            }
        }

        for (DestinationMapPoi p : d.getMapPois()) {
            if ("hotel".equals(p.getCategory()) && p.getTier() == null) {
                p.setTier(inferHotelTier(p.getName(), p.getPriceLabel()));
                changed = true;
            }
            if (p.getLatitude() == null && "bangkok".equals(d.getSlug())) {
                assignBangkokCoords(p);
                changed = true;
            }
        }

        if ("bangkok".equals(d.getSlug())) {
            changed |= ensureBangkokPois(d);
        }
        if ("phuket".equals(d.getSlug())) {
            changed |= ensurePhuketPois(d);
        }

        if (changed) {
            destinationRepository.save(d);
            log.info("Enhanced destination {}", d.getSlug());
        }
    }

    private String inferHotelTier(String name, String price) {
        if (name != null && (name.contains("Siam") || name.contains("Trisara"))) {
            return "luxury";
        }
        if (price != null && (price.contains("650") || price.contains("500"))) {
            return "budget";
        }
        if (price != null && price.contains("2.500")) {
            return "luxury";
        }
        return "mid";
    }

    private void assignBangkokCoords(DestinationMapPoi p) {
        if ("The Siam Bangkok".equals(p.getName())) {
            p.setLatitude(13.762);
            p.setLongitude(100.505);
        } else if ("Jay Fai".equals(p.getName())) {
            p.setLatitude(13.754);
            p.setLongitude(100.516);
        }
    }

    private boolean ensureBangkokPois(Destination d) {
        List<String> existing = d.getMapPois().stream().map(DestinationMapPoi::getName).toList();
        boolean added = false;
        if (!existing.contains("Lub d Bangkok Siam")) {
            d.getMapPois().add(poi(d, "hotel", "budget", "Lub d Bangkok Siam", "4.2", "650.000đ/đêm", 13.745, 100.534, 10));
            added = true;
        }
        if (!existing.contains("Novotel Bangkok Sukhumvit")) {
            d.getMapPois().add(poi(d, "hotel", "mid", "Novotel Bangkok Sukhumvit", "4.5", "1.200.000đ/đêm", 13.738, 100.560, 11));
            added = true;
        }
        if (d.getMapPois().stream().anyMatch(p -> "The Siam Bangkok".equals(p.getName()))) {
            d.getMapPois().stream()
                    .filter(p -> "The Siam Bangkok".equals(p.getName()))
                    .forEach(p -> {
                        if (p.getTier() == null) p.setTier("luxury");
                    });
        }
        if (!existing.contains("BTS Siam")) {
            d.getMapPois().add(poi(d, "transport", null, "BTS Siam", "4.5", "35–60 THB/lượt", 13.745, 100.534, 12));
            added = true;
        }
        return added;
    }

    private boolean ensurePhuketPois(Destination d) {
        long hotels = d.getMapPois().stream().filter(p -> "hotel".equals(p.getCategory())).count();
        if (hotels >= 3) return false;
        d.getMapPois().add(poi(d, "hotel", "budget", "Patong Lodge", "4.1", "500.000đ/đêm", 7.896, 98.298, 20));
        d.getMapPois().add(poi(d, "hotel", "mid", "Amari Phuket", "4.6", "1.800.000đ/đêm", 7.900, 98.305, 21));
        d.getMapPois().add(poi(d, "hotel", "luxury", "Trisara Phuket", "4.9", "8.000.000đ/đêm", 7.980, 98.280, 22));
        return true;
    }

    private DestinationMapPoi poi(Destination d, String cat, String tier, String name, String rating,
            String price, double lat, double lng, int ord) {
        return DestinationMapPoi.builder()
                .destination(d).category(cat).tier(tier).name(name)
                .rating(new BigDecimal(rating)).priceLabel(price)
                .latitude(lat).longitude(lng).sortOrder(ord)
                .build();
    }
}

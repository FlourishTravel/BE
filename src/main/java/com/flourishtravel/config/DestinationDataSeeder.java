package com.flourishtravel.config;

import com.flourishtravel.domain.destination.entity.*;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class DestinationDataSeeder {

    private final DestinationRepository destinationRepository;
    private final ThaiFestivalRepository festivalRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(4)
    @Transactional
    public void seed() {
        if (destinationRepository.count() > 0) {
            return;
        }
        log.info("Seeding Thailand destinations...");
        seedBangkok();
        seedPhuket();
        seedChiangMai();
        seedKrabi();
        seedPattaya();
        seedFestivals();
        log.info("Destination seed completed.");
    }

    private static final String YT_BANGKOK = "https://www.youtube.com/watch?v=7XNMzqNqDuE";
    private static final String YT_PHUKET = "https://www.youtube.com/watch?v=Kp0eYw85MfY";
    private static final String YT_LOY = "https://www.youtube.com/watch?v=0fbiqvK2s8k";
    private static final String YT_SONGKRAN = "https://www.youtube.com/watch?v=dxp9w9Ggehc";

    private void seedFestivals() {
        festivalRepository.save(ThaiFestival.builder()
                .slug("loy-krathong")
                .name("Loy Krathong")
                .monthLabel("Tháng 11 hàng năm")
                .description("Lễ hội thả đèn hoa đăng lung linh lãng mạn nhất tại Thái Lan.")
                .longDescription("Loy Krathong diễn ra vào đêm trăng tròn tháng 12 âm lịch. Du khách thả krathong (đèn hoa) xuống sông để cầu may và tạ ơn nước. Chiang Mai và Bangkok là hai điểm đón lễ hội sôi động nhất.")
                .relatedDestinationSlug("chiang-mai")
                .videoUrl(YT_LOY)
                .tips("Đặt khách sạn sớm — giá tăng 30–50% ở Chiang Mai\nMặc trang phục thoải mái, tránh vải dễ cháy gần đèn nến\nTham gia thả krathong tại bờ sông Chao Phraya hoặc hồ Kumphaphi")
                .imageUrl("https://images.unsplash.com/photo-1476511326534-6bee0e191a0b?w=800&q=80")
                .sortOrder(1)
                .build());
        festivalRepository.save(ThaiFestival.builder()
                .slug("songkran")
                .name("Songkran")
                .monthLabel("Tháng 4 hàng năm")
                .description("Lễ hội té nước tưng bừng, đón năm mới theo phong cách Thái Lan.")
                .longDescription("Songkran là Tết cổ truyền Thái Lan (13–15/4). Bangkok, Chiang Mai và Pattaya có các điểm té nước công cộng. Đây là mùa cao điểm du lịch nội địa và quốc tế.")
                .relatedDestinationSlug("bangkok")
                .videoUrl(YT_SONGKRAN)
                .tips("Bọc điện thoại và túi tiền trong túi chống nước\nKhông té nước vào người già, trẻ em hoặc tu sĩ\nKhao San Road và Silom là khu vực sôi động nhất ở Bangkok")
                .imageUrl("https://images.unsplash.com/photo-1528183429752-a97d0bf99b5a?w=800&q=80")
                .sortOrder(2)
                .build());
    }

    private void seedBangkok() {
        Destination d = baseDestination("bangkok", "Bangkok", 1,
                "city,culture,food,shopping,nightlife,family,couple,backpacker",
                "Thủ đô sôi động với chùa chiền, ẩm thực đường phố và trung tâm mua sắm hiện đại.",
                "https://images.unsplash.com/photo-1563492065-608b2d5b4f52?w=1200&q=80",
                new BigDecimal("4.8"), 8, 15, 31, 3, 5, "Cả năm",
                13.7563, 100.5018, 92);
        d.setDescription("Bangkok là điểm đến lý tưởng cho lần đầu đến Thái Lan: Grand Palace, Wat Pho, ICONSIAM và chợ Chatuchak.");
        d.setVideoUrl(YT_BANGKOK);
        d.setLocationLabel("Miền Trung Thái Lan");
        d.setWeatherNow("31°C, nắng nhẹ");
        d.setWeatherForecast("Tuần tới: 29–33°C, mưa rào ngắn buổi chiều");
        addHighlights(d, "Grand Palace", "Wat Arun", "ICONSIAM", "Jodd Fairs");
        addCosts(d);
        addAttraction(d, "Grand Palace", "Cung điện Hoàng gia", "500–1.000 THB", "08:30–15:30", 13.751, 100.492, 1);
        addAttraction(d, "Wat Pho", "Chùa Phật nằm", "200 THB", "08:00–18:30", 13.746, 100.493, 2);
        addAttraction(d, "Wat Arun", "Chùa Bình Minh", "100 THB", "08:00–18:00", 13.744, 100.488, 3);
        addAttraction(d, "ICONSIAM", "Trung tâm mua sắm ven sông", "Miễn phí", "10:00–22:00", 13.726, 100.510, 4);
        addMapPoi(d, "hotel", "budget", "Lub d Bangkok Siam", "4.2", "650.000đ/đêm", 13.745, 100.534, 1);
        addMapPoi(d, "hotel", "mid", "Novotel Bangkok Sukhumvit", "4.5", "1.200.000đ/đêm", 13.738, 100.560, 2);
        addMapPoi(d, "hotel", "luxury", "The Siam Bangkok", "4.9", "2.500.000đ/đêm", 13.762, 100.505, 3);
        addMapPoi(d, "restaurant", null, "Jay Fai", "4.7", "800.000đ/người", 13.754, 100.516, 4);
        addMapPoi(d, "restaurant", null, "Thipsamai Pad Thai", "4.6", "150.000đ/người", 13.752, 100.505, 5);
        addMapPoi(d, "attraction", null, "Grand Palace", "4.8", "500–1.000 THB", 13.751, 100.492, 6);
        addMapPoi(d, "transport", null, "BTS Siam", "4.5", "35–60 THB/lượt", 13.745, 100.534, 7);
        addMapPoi(d, "shopping", null, "ICONSIAM", "4.7", "Miễn phí vào cửa", 13.726, 100.510, 8);
        addReview(d, "Lan Anh", "5", "Bangkok rất phù hợp cho lần đầu đi Thái Lan!", 1);
        destinationRepository.save(d);
    }

    private void seedPhuket() {
        Destination d = baseDestination("phuket", "Phuket", 2,
                "beach,couple,family,nightlife,food",
                "Thiên đường biển đảo với bãi Patong, Phi Phi và tượng Phật Big Buddha.",
                "https://images.unsplash.com/photo-1552465011-b21e5687a93c?w=1200&q=80",
                new BigDecimal("4.9"), 10, 18, 30, 4, 6, "Tháng 11–Tháng 4",
                7.8804, 98.3923, 90);
        d.setVideoUrl(YT_PHUKET);
        addHighlights(d, "Patong Beach", "Phi Phi Island", "Big Buddha");
        addCosts(d);
        addAttraction(d, "Patong Beach", "Bãi biển sôi động", "Miễn phí", "Cả ngày", 7.896, 98.296, 1);
        addAttraction(d, "Phi Phi Island", "Tour đảo trong ngày", "1.500–2.500 THB", "08:00–17:00", 7.740, 98.778, 2);
        addMapPoi(d, "hotel", "budget", "Patong Lodge", "4.1", "500.000đ/đêm", 7.896, 98.298, 1);
        addMapPoi(d, "hotel", "mid", "Amari Phuket", "4.6", "1.800.000đ/đêm", 7.900, 98.305, 2);
        addMapPoi(d, "hotel", "luxury", "Trisara Phuket", "4.9", "8.000.000đ/đêm", 7.980, 98.280, 3);
        addMapPoi(d, "attraction", null, "Big Buddha", "4.8", "Miễn phí", 7.827, 98.312, 4);
        addReview(d, "Minh Tuấn", "5", "Biển đẹp, hải sản tươi ngon!", 1);
        destinationRepository.save(d);
    }

    private void seedChiangMai() {
        Destination d = baseDestination("chiang-mai", "Chiang Mai", 3,
                "culture,family,couple,backpacker,food",
                "Thành phố rose của phương Bắc với Doi Suthep, night bazaar và làng nghệ thuật.",
                "https://images.unsplash.com/photo-1552465011-9ce21d7c9aef?w=1200&q=80",
                new BigDecimal("4.7"), 6, 12, 28, 3, 5, "Tháng 11–Tháng 2",
                18.7883, 98.9853, 88);
        addHighlights(d, "Doi Suthep", "Night Bazaar", "Elephant Sanctuary");
        addCosts(d);
        addAttraction(d, "Doi Suthep", "Chùa trên núi", "50 THB", "06:00–18:00", 1);
        addReview(d, "Hương", "4.8", "Không khí bình yên, ẩm thực Bắc Thái tuyệt vời.", 1);
        destinationRepository.save(d);
    }

    private void seedKrabi() {
        Destination d = baseDestination("krabi", "Krabi", 4,
                "beach,couple,backpacker,family",
                "Vịnh đá vôi hùng vĩ với Railay, Ao Nang và Hong Island.",
                "https://images.unsplash.com/photo-1519046909334-30407b0bd0c0?w=1200&q=80",
                new BigDecimal("4.8"), 8, 14, 29, 3, 5, "Tháng 11–Tháng 4",
                8.0863, 98.9063, 89);
        addHighlights(d, "Railay Beach", "Ao Nang", "Hong Island");
        addCosts(d);
        addAttraction(d, "Railay Beach", "Bãi biển đá vôi", "Miễn phí", "Cả ngày", 1);
        destinationRepository.save(d);
    }

    private void seedPattaya() {
        Destination d = baseDestination("pattaya", "Pattaya", 5,
                "beach,nightlife,couple,family",
                "Thành phố biển gần Bangkok với Walking Street, Coral Island và Sanctuary of Truth.",
                "https://images.unsplash.com/photo-1528183429752-a97d0bf99b5a?w=1200&q=80",
                new BigDecimal("4.6"), 7, 13, 30, 2, 4, "Cả năm",
                12.9236, 100.8825, 85);
        addHighlights(d, "Walking Street", "Coral Island", "Sanctuary of Truth");
        addCosts(d);
        destinationRepository.save(d);
    }

    private Destination baseDestination(String slug, String name, int order, String types,
            String summary, String hero, BigDecimal rating,
            int costMin, int costMax, int temp, int daysMin, int daysMax, String bestTime,
            double lat, double lng, int floraMatch) {
        return Destination.builder()
                .slug(slug).name(name).sortOrder(order).types(types).summary(summary)
                .heroImageUrl(hero).rating(rating)
                .avgCostMinMillion(costMin).avgCostMaxMillion(costMax)
                .avgTemperatureC(temp).idealDaysMin(daysMin).idealDaysMax(daysMax)
                .bestTimeLabel(bestTime).latitude(lat).longitude(lng)
                .floraMatchDefault(floraMatch).featured(true).published(true)
                .timezone("UTC+7").language("Tiếng Thái").currency("THB (Baht)")
                .build();
    }

    private void addHighlights(Destination d, String... names) {
        int i = 0;
        for (String n : names) {
            d.getHighlightSpots().add(DestinationHighlightSpot.builder().destination(d).name(n).sortOrder(++i).build());
        }
    }

    private void addCosts(Destination d) {
        d.getCostItems().add(item(d, "flight", "Vé máy bay", 2, 6, 1));
        d.getCostItems().add(item(d, "hotel", "Khách sạn", 0, 2, 2));
        d.getCostItems().add(item(d, "food", "Ăn uống", 0, 1, 3));
        d.getCostItems().add(item(d, "transport", "Di chuyển", 0, 0, 4));
    }

    private DestinationCostItem item(Destination d, String cat, String label, int min, int max, int ord) {
        return DestinationCostItem.builder().destination(d).category(cat).label(label)
                .costMinMillion(min).costMaxMillion(max).sortOrder(ord).build();
    }

    private void addAttraction(Destination d, String name, String desc, String ticket, String hours, int ord) {
        addAttraction(d, name, desc, ticket, hours, null, null, ord);
    }

    private void addAttraction(Destination d, String name, String desc, String ticket, String hours,
            Double lat, Double lng, int ord) {
        d.getAttractions().add(DestinationAttraction.builder()
                .destination(d).name(name).description(desc)
                .ticketPriceLabel(ticket).openHours(hours).sortOrder(ord)
                .latitude(lat).longitude(lng)
                .imageUrl(d.getHeroImageUrl())
                .build());
    }

    private void addMapPoi(Destination d, String cat, String tier, String name, String rating, String price,
            double lat, double lng, int ord) {
        d.getMapPois().add(DestinationMapPoi.builder()
                .destination(d).category(cat).tier(tier).name(name)
                .rating(new BigDecimal(rating)).priceLabel(price)
                .latitude(lat).longitude(lng).sortOrder(ord)
                .build());
    }

    private void addReview(Destination d, String author, String rating, String comment, int ord) {
        d.getReviews().add(DestinationReview.builder()
                .destination(d).authorName(author)
                .rating(new BigDecimal(rating)).comment(comment).sortOrder(ord)
                .build());
    }
}

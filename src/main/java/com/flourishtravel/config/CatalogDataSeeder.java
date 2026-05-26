package com.flourishtravel.config;

import com.flourishtravel.domain.catalog.entity.TravelTicket;
import com.flourishtravel.domain.catalog.repository.TravelTicketRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogDataSeeder {

    private final TravelTicketRepository ticketRepository;
    private final TourRepository tourRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(6)
    @Transactional
    public void seed() {
        seedTickets();
        enhanceThailandTours();
    }

    private void seedTickets() {
        if (ticketRepository.count() > 0) return;
        log.info("Seeding travel tickets...");
        int o = 1;
        ticketRepository.save(ticket("safari-world-bangkok", "Safari World Bangkok", "attraction", "Bangkok",
                "Khám phá thiên nhiên hoang dã", "650 THB", new BigDecimal("900000"), "4.8",
                "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?w=600&q=80", true, o++));
        ticketRepository.save(ticket("grand-palace", "Grand Palace", "attraction", "Bangkok",
                "Cung điện Hoàng gia", "500–1.000 THB", new BigDecimal("750000"), "4.9",
                "https://images.unsplash.com/photo-1563492065-608b2d5b4f52?w=600&q=80", true, o++));
        ticketRepository.save(ticket("sea-life-bangkok", "Sea Life Bangkok Ocean World", "attraction", "Bangkok",
                "Thủy cung trong nhà Siam Paragon", "Từ 990 THB", new BigDecimal("850000"), "4.7",
                "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=600&q=80", true, o++));
        ticketRepository.save(ticket("alcazar-show", "Alcazar Show Pattaya", "show", "Pattaya",
                "Show ladyboy nổi tiếng", "Từ 800 THB", new BigDecimal("650000"), "4.6",
                "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&q=80", true, o++));
        ticketRepository.save(ticket("tiffany-show", "Tiffany's Show", "show", "Pattaya",
                "Show cabaret Pattaya", "Từ 900 THB", new BigDecimal("700000"), "4.7",
                "https://images.unsplash.com/photo-1503090832-4f1db25b9e06?w=600&q=80", false, o++));
        ticketRepository.save(ticket("bts-day-pass", "BTS Day Pass Bangkok", "transport", "Bangkok",
                "Vé một ngày BTS Skytrain", "Từ 140 THB", new BigDecimal("120000"), "4.5",
                "https://images.unsplash.com/photo-1519005053897-7e8a4f17f81f?w=600&q=80", false, o++));
        ticketRepository.save(ticket("flight-sgn-bkk", "TP.HCM → Bangkok", "flight", "Bangkok",
                "Chuyến bay khứ hồi gợi ý", "Từ 2.3 triệu", new BigDecimal("2300000"), "4.4",
                "https://images.unsplash.com/photo-1436491865339-9a61a109fc85?w=600&q=80", true, o++));
        log.info("Travel tickets seeded.");
    }

    private void enhanceThailandTours() {
        List<Tour> tours = tourRepository.findAll();
        for (Tour t : tours) {
            String title = t.getTitle() != null ? t.getTitle().toLowerCase() : "";
            if (!title.contains("bangkok") && !title.contains("phuket") && !title.contains("pattaya")
                    && !title.contains("chiang") && !title.contains("samui") && !title.contains("ayutthaya")) {
                continue;
            }
            if (t.getDestinationCity() == null) {
                if (title.contains("bangkok")) t.setDestinationCity("Bangkok");
                else if (title.contains("phuket")) t.setDestinationCity("Phuket");
                else if (title.contains("pattaya")) t.setDestinationCity("Pattaya");
                else if (title.contains("chiang")) t.setDestinationCity("Chiang Mai");
                else if (title.contains("samui")) t.setDestinationCity("Koh Samui");
            }
            if (t.getRating() == null) t.setRating(new BigDecimal("4.8"));
            if (title.contains("pattaya") && title.contains("bangkok")) {
                t.setFeatured(true);
                t.setBadge("bestseller");
                t.setTags("Khách sạn 4 sao,HDV Tiếng Việt,Xe đưa đón");
            } else if (title.contains("phuket") && title.contains("luxury")) {
                t.setBadge("premium");
                t.setTags("Resort 5 sao,HDV,Ăn uống");
            } else {
                t.setTags("Khách sạn,HDV Tiếng Việt,Xe đưa đón");
            }
            if (t.getHighlightsText() == null) {
                t.setHighlightsText("Grand Palace\nICONSIAM\nBãi biển đẹp\nẨm thực đường phố");
            }
            if (t.getIncludesText() == null) {
                t.setIncludesText("Khách sạn tiêu chuẩn\nXe đưa đón sân bay\nHướng dẫn viên tiếng Việt\nVé tham quan theo chương trình");
            }
            if (t.getExcludesText() == null) {
                t.setExcludesText("Vé máy bay quốc tế\nChi phí cá nhân\nTiền tip");
            }
            tourRepository.save(t);
        }
    }

    private TravelTicket ticket(String slug, String name, String cat, String city, String desc,
            String priceLabel, BigDecimal priceVnd, String rating, String img, boolean featured, int order) {
        return TravelTicket.builder()
                .slug(slug).name(name).category(cat).destinationCity(city)
                .shortDescription(desc).priceLabel(priceLabel).priceVnd(priceVnd)
                .rating(new BigDecimal(rating)).imageUrl(img).eTicket(true)
                .featured(featured).published(true).sortOrder(order)
                .locationLabel(city).build();
    }
}

package com.flourishtravel.domain.catalog.service;

import com.flourishtravel.domain.catalog.dto.*;
import com.flourishtravel.domain.catalog.entity.TravelTicket;
import com.flourishtravel.domain.catalog.repository.TravelTicketRepository;
import com.flourishtravel.domain.tour.dto.TourDetailDto;
import com.flourishtravel.domain.tour.dto.TourSummaryDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final TourService tourService;
    private final TravelTicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public CatalogPageDto search(String destination, LocalDate startDate, Integer guests,
            String catalogType, int tourPage, int tourSize) {
        String dest = normalizeDestination(destination);
        String ticketCategory = mapCatalogTypeToTicketCategory(catalogType);

        Page<TourSummaryDto> tourPageResult = tourService.publicCatalogBrowse(
                dest, null, null, null, null, PageRequest.of(tourPage, Math.min(tourSize, 24)));

        List<TourCardDto> tours = tourPageResult.getContent().stream()
                .map(this::toTourCard)
                .filter(t -> matchesCatalogType(t, catalogType))
                .toList();

        List<TourCardDto> featured = tours.stream().filter(t -> Boolean.TRUE.equals(t.getFeatured())).toList();
        if (featured.isEmpty()) {
            featured = tours.stream().limit(3).toList();
        }

        List<TicketCardDto> tickets = List.of();
        if (catalogType == null || catalogType.isBlank() || "combo".equals(catalogType)
                || !"tour".equals(catalogType)) {
            try {
                String ticketCat = "tour".equals(catalogType) ? null : ticketCategory;
                tickets = ticketRepository.search(ticketCat, dest).stream()
                        .map(this::toTicketCard)
                        .limit(12)
                        .toList();
            } catch (Exception ex) {
                tickets = List.of();
            }
        }

        FloraTourRecommendDto flora = buildFloraSuggestion(null, dest);

        return CatalogPageDto.builder()
                .featuredTours(featured)
                .tours(tours)
                .tickets(tickets)
                .floraSuggestion(flora)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TicketCardDto> listTickets(String category, String destination) {
        return ticketRepository.search(
                mapCatalogTypeToTicketCategory(category),
                normalizeDestination(destination))
                .stream().map(this::toTicketCard).toList();
    }

    @Transactional(readOnly = true)
    public TicketCardDto getTicketBySlug(String slug) {
        TravelTicket t = ticketRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new com.flourishtravel.common.exception.ResourceNotFoundException("Ticket not found: " + slug));
        return toTicketCard(t);
    }

    @Transactional(readOnly = true)
    public FloraTourRecommendDto floraRecommend(FloraTourRecommendRequest req) {
        return buildFloraSuggestion(req.getBudgetVnd(), normalizeDestination(req.getDestination()));
    }

    @Transactional(readOnly = true)
    public TourDetailDto enrichTourDetail(TourDetailDto detail) {
        if (detail == null) return null;
        Tour tour = tourService.getById(detail.getId());
        detail.setDestinationCity(resolveDestinationCity(tour));
        detail.setRating(tour.getRating() != null ? tour.getRating() : new BigDecimal("4.8"));
        detail.setBadge(tour.getBadge());
        detail.setTags(splitTags(tour.getTags()));
        detail.setHighlights(splitLines(tour.getHighlightsText()));
        detail.setIncludes(splitLines(tour.getIncludesText()));
        detail.setExcludes(splitLines(tour.getExcludesText()));
        detail.setReviews(mockReviews(detail.getTitle()));
        if (detail.getThumbnailUrl() == null && detail.getImages() != null && !detail.getImages().isEmpty()) {
            detail.setThumbnailUrl(detail.getImages().get(0).getImageUrl());
        }
        return detail;
    }

    private TourCardDto toTourCard(TourSummaryDto s) {
        Tour tour = null;
        try {
            tour = tourService.getById(s.getId());
        } catch (Exception ignored) {
        }
        String dest = tour != null ? resolveDestinationCity(tour) : inferDestinationFromTitle(s.getTitle());
        List<String> tags = tour != null ? splitTags(tour.getTags()) : defaultTagsForTour(s.getTitle());
        return TourCardDto.builder()
                .id(s.getId())
                .title(s.getTitle())
                .slug(s.getSlug())
                .description(s.getDescription())
                .basePrice(s.getBasePrice())
                .priceFromLabel(s.getBasePrice() != null
                        ? "Từ " + formatMillion(s.getBasePrice()) : null)
                .durationDays(s.getDurationDays())
                .durationNights(s.getDurationNights())
                .durationLabel(formatDuration(s.getDurationDays(), s.getDurationNights()))
                .thumbnailUrl(s.getThumbnailUrl())
                .destinationCity(dest)
                .locationLabel(dest)
                .rating(tour != null && tour.getRating() != null ? tour.getRating() : new BigDecimal("4.8"))
                .badge(tour != null ? tour.getBadge() : null)
                .tags(tags)
                .featured(tour != null && Boolean.TRUE.equals(tour.getFeatured()))
                .status(s.getStatus())
                .build();
    }

    private TicketCardDto toTicketCard(TravelTicket t) {
        return TicketCardDto.builder()
                .id(t.getId())
                .slug(t.getSlug())
                .name(t.getName())
                .category(t.getCategory())
                .destinationCity(t.getDestinationCity())
                .shortDescription(t.getShortDescription())
                .imageUrl(t.getImageUrl())
                .priceVnd(t.getPriceVnd())
                .priceLabel(t.getPriceLabel())
                .rating(t.getRating())
                .showTimeLabel(t.getShowTimeLabel())
                .locationLabel(t.getLocationLabel())
                .routeLabel(t.getRouteLabel())
                .eTicket(t.getETicket())
                .featured(t.getFeatured())
                .build();
    }

    private FloraTourRecommendDto buildFloraSuggestion(Long budgetVnd, String destination) {
        String dest = destination != null ? destination : "Bangkok";
        Page<TourSummaryDto> page = tourService.publicCatalogBrowse(dest, null, null, null, null, PageRequest.of(0, 20));
        TourSummaryDto pick = page.getContent().stream()
                .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains("pattaya"))
                .findFirst()
                .orElse(page.getContent().isEmpty() ? null : page.getContent().get(0));

        if (pick == null) {
            return FloraTourRecommendDto.builder()
                    .message("Chưa có tour phù hợp — thử đổi điểm đến.")
                    .budgetVnd(budgetVnd)
                    .matchPercent(0)
                    .build();
        }

        long budget = budgetVnd != null ? budgetVnd : 10_000_000L;
        int match = pick.getBasePrice() != null && pick.getBasePrice().longValue() <= budget ? 92 : 78;

        return FloraTourRecommendDto.builder()
                .message("Với ngân sách " + formatMillion(BigDecimal.valueOf(budget)) + ", Flora đề xuất tour sau:")
                .budgetVnd(budget)
                .tourId(pick.getId())
                .tourTitle(pick.getTitle())
                .tourSlug(pick.getSlug())
                .durationLabel(formatDuration(pick.getDurationDays(), pick.getDurationNights()))
                .priceVnd(pick.getBasePrice())
                .matchPercent(match)
                .build();
    }

    private boolean matchesCatalogType(TourCardDto t, String catalogType) {
        if (catalogType == null || catalogType.isBlank() || "tour".equals(catalogType) || "combo".equals(catalogType)) {
            return true;
        }
        return false;
    }

    private String mapCatalogTypeToTicketCategory(String catalogType) {
        if (catalogType == null) return null;
        return switch (catalogType) {
            case "attraction", "ticket" -> "attraction";
            case "show" -> "show";
            case "transport" -> "transport";
            case "flight" -> "flight";
            default -> null;
        };
    }

    private String normalizeDestination(String d) {
        if (d == null || d.isBlank()) return null;
        String s = d.trim();
        String lower = s.toLowerCase();
        if (lower.contains("bangkok")) return "Bangkok";
        if (lower.contains("phuket")) return "Phuket";
        if (lower.contains("pattaya")) return "Pattaya";
        if (lower.contains("chiang mai") || lower.equals("chiang-mai")) return "Chiang Mai";
        if (lower.contains("chiang rai") || lower.equals("chiang-rai")) return "Chiang Rai";
        if (lower.contains("krabi")) return "Krabi";
        if (lower.contains("samui")) return "Koh Samui";
        if (lower.contains("ayutthaya")) return "Ayutthaya";
        if (lower.contains("hua hin") || lower.equals("hua-hin")) return "Hua Hin";
        return s;
    }

    private String resolveDestinationCity(Tour tour) {
        if (tour.getDestinationCity() != null && !tour.getDestinationCity().isBlank()) {
            return tour.getDestinationCity();
        }
        return inferDestinationFromTitle(tour.getTitle());
    }

    private String inferDestinationFromTitle(String title) {
        if (title == null) return "Thái Lan";
        String t = title.toLowerCase();
        if (t.contains("bangkok")) return "Bangkok";
        if (t.contains("phuket")) return "Phuket";
        if (t.contains("pattaya")) return "Pattaya";
        if (t.contains("chiang mai") || t.contains("chiang-mai")) return "Chiang Mai";
        if (t.contains("samui")) return "Koh Samui";
        if (t.contains("ayutthaya")) return "Ayutthaya";
        return "Thái Lan";
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private List<String> defaultTagsForTour(String title) {
        List<String> tags = new ArrayList<>();
        tags.add("HDV Tiếng Việt");
        if (title != null && title.toLowerCase().contains("bangkok")) {
            tags.add("Khách sạn 4 sao");
            tags.add("Xe đưa đón");
        }
        return tags;
    }

    private String formatDuration(Integer days, Integer nights) {
        int d = days != null ? days : 0;
        int n = nights != null ? nights : Math.max(0, d - 1);
        if (d <= 0) return "";
        return n > 0 ? d + " Ngày " + n + " Đêm" : d + " Ngày";
    }

    private String formatMillion(BigDecimal vnd) {
        if (vnd == null) return "—";
        long m = vnd.longValue() / 1_000_000L;
        if (m >= 1000) return String.format("%.1f tỷ", vnd.longValue() / 1_000_000_000.0);
        return m + " triệu";
    }

    private List<TourDetailDto.ReviewRef> mockReviews(String tourTitle) {
        return List.of(
                TourDetailDto.ReviewRef.builder()
                        .authorName("Lan Anh")
                        .rating(new BigDecimal("5"))
                        .comment("Tour " + (tourTitle != null ? tourTitle : "") + " rất worth it, HDV nhiệt tình!")
                        .build(),
                TourDetailDto.ReviewRef.builder()
                        .authorName("Minh Tuấn")
                        .rating(new BigDecimal("4.5"))
                        .comment("Lịch trình hợp lý, khách sạn sạch, ăn uống ngon.")
                        .build()
        );
    }
}

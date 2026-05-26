package com.flourishtravel.domain.destination.service;

import com.flourishtravel.domain.destination.dto.*;
import com.flourishtravel.domain.destination.entity.*;
import com.flourishtravel.domain.destination.repository.DestinationRepository;
import com.flourishtravel.domain.destination.repository.ThaiFestivalRepository;
import com.flourishtravel.domain.tour.dto.TourSummaryDto;
import com.flourishtravel.domain.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final ThaiFestivalRepository festivalRepository;
    private final TourService tourService;

    @Transactional(readOnly = true)
    public List<DestinationSummaryDto> list(String type, String q) {
        return destinationRepository.findPublished(type, q).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public DestinationDetailDto getBySlug(String slug) {
        Destination d = destinationRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy điểm đến"));
        return toDetail(d);
    }

    @Transactional(readOnly = true)
    public List<ThaiFestivalDto> listFestivals() {
        return festivalRepository.findByPublishedTrueOrderBySortOrderAsc().stream()
                .map(this::toFestivalSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ThaiFestivalDetailDto getFestival(String slug) {
        ThaiFestival f = festivalRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lễ hội"));
        String relatedName = null;
        if (f.getRelatedDestinationSlug() != null && !f.getRelatedDestinationSlug().isBlank()) {
            relatedName = destinationRepository.findBySlugAndPublishedTrue(f.getRelatedDestinationSlug())
                    .map(Destination::getName)
                    .orElse(null);
        }
        return ThaiFestivalDetailDto.builder()
                .id(f.getId())
                .slug(f.getSlug())
                .name(f.getName())
                .monthLabel(f.getMonthLabel())
                .description(f.getDescription())
                .longDescription(f.getLongDescription())
                .imageUrl(f.getImageUrl())
                .videoUrl(f.getVideoUrl())
                .relatedDestinationSlug(f.getRelatedDestinationSlug())
                .relatedDestinationName(relatedName)
                .tips(parseTips(f.getTips()))
                .build();
    }

    private ThaiFestivalDto toFestivalSummary(ThaiFestival f) {
        return ThaiFestivalDto.builder()
                .id(f.getId())
                .slug(f.getSlug())
                .name(f.getName())
                .monthLabel(f.getMonthLabel())
                .description(f.getDescription())
                .imageUrl(f.getImageUrl())
                .build();
    }

    private List<String> parseTips(String tips) {
        if (tips == null || tips.isBlank()) return List.of();
        return Arrays.stream(tips.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public MapStatsDto mapStats(String slug) {
        Destination d = destinationRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy điểm đến"));
        long hotels = d.getMapPois().stream().filter(p -> "hotel".equals(p.getCategory())).count();
        long restaurants = d.getMapPois().stream().filter(p -> "restaurant".equals(p.getCategory())).count();
        long attractions = d.getMapPois().stream().filter(p -> "attraction".equals(p.getCategory())).count();
        return MapStatsDto.builder().hotels(hotels).restaurants(restaurants).attractions(attractions).build();
    }

    @Transactional(readOnly = true)
    public DestinationDetailDto.FloraMatchDto floraMatch(FloraMatchRequest request) {
        List<String> prefs = request.getPreferences() != null ? request.getPreferences() : List.of();
        String slug = request.getDestinationSlug();
        if (slug == null || slug.isBlank()) {
            slug = "bangkok";
        }
        Destination d = destinationRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy điểm đến"));

        int base = d.getFloraMatchDefault() != null ? d.getFloraMatchDefault() : 85;
        int bonus = Math.min(10, prefs.size() * 3);
        int match = Math.min(99, base + bonus);

        String prefText = prefs.isEmpty() ? "du lịch đa dạng" : String.join(", ", prefs);
        return DestinationDetailDto.FloraMatchDto.builder()
                .destinationSlug(d.getSlug())
                .destinationName(d.getName())
                .matchPercent(match)
                .matchedPreferences(prefs)
                .message("Dựa trên sở thích của bạn (" + prefText + "), " + d.getName() + " phù hợp " + match + "%!")
                .build();
    }

    private DestinationSummaryDto toSummary(Destination d) {
        return DestinationSummaryDto.builder()
                .id(d.getId())
                .slug(d.getSlug())
                .name(d.getName())
                .summary(d.getSummary())
                .heroImageUrl(d.getHeroImageUrl())
                .rating(d.getRating())
                .avgCostMinMillion(d.getAvgCostMinMillion())
                .avgCostMaxMillion(d.getAvgCostMaxMillion())
                .avgTemperatureC(d.getAvgTemperatureC())
                .idealDaysMin(d.getIdealDaysMin())
                .idealDaysMax(d.getIdealDaysMax())
                .bestTimeLabel(d.getBestTimeLabel())
                .types(splitTypes(d.getTypes()))
                .highlightSpots(d.getHighlightSpots().stream()
                        .sorted(Comparator.comparingInt(h -> h.getSortOrder() != null ? h.getSortOrder() : 0))
                        .map(DestinationHighlightSpot::getName)
                        .toList())
                .build();
    }

    private DestinationDetailDto toDetail(Destination d) {
        List<DestinationDetailDto.TourSuggestionDto> tours = tourService
                .publicCatalog(d.getName(), null, null, null, null, PageRequest.of(0, 6))
                .getContent().stream()
                .map(this::tourToSuggestion)
                .toList();

        return DestinationDetailDto.builder()
                .id(d.getId())
                .slug(d.getSlug())
                .name(d.getName())
                .summary(d.getSummary())
                .description(d.getDescription())
                .heroImageUrl(d.getHeroImageUrl())
                .videoUrl(d.getVideoUrl())
                .rating(d.getRating())
                .avgCostMinMillion(d.getAvgCostMinMillion())
                .avgCostMaxMillion(d.getAvgCostMaxMillion())
                .avgTemperatureC(d.getAvgTemperatureC())
                .idealDaysMin(d.getIdealDaysMin())
                .idealDaysMax(d.getIdealDaysMax())
                .bestTimeLabel(d.getBestTimeLabel())
                .locationLabel(d.getLocationLabel())
                .timezone(d.getTimezone())
                .language(d.getLanguage())
                .currency(d.getCurrency())
                .weatherNow(d.getWeatherNow())
                .weatherForecast(d.getWeatherForecast())
                .latitude(d.getLatitude())
                .longitude(d.getLongitude())
                .types(splitTypes(d.getTypes()))
                .highlightSpots(d.getHighlightSpots().stream()
                        .sorted(Comparator.comparingInt(h -> h.getSortOrder() != null ? h.getSortOrder() : 0))
                        .map(DestinationHighlightSpot::getName).toList())
                .attractions(d.getAttractions().stream()
                        .sorted(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0))
                        .map(a -> DestinationDetailDto.AttractionDto.builder()
                                .id(a.getId()).name(a.getName()).description(a.getDescription())
                                .imageUrl(a.getImageUrl()).ticketPriceLabel(a.getTicketPriceLabel())
                                .openHours(a.getOpenHours()).latitude(a.getLatitude()).longitude(a.getLongitude())
                                .build()).toList())
                .costItems(d.getCostItems().stream()
                        .sorted(Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0))
                        .map(c -> DestinationDetailDto.CostItemDto.builder()
                                .category(c.getCategory()).label(c.getLabel())
                                .costMinMillion(c.getCostMinMillion()).costMaxMillion(c.getCostMaxMillion())
                                .build()).toList())
                .mapPois(d.getMapPois().stream()
                        .sorted(Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0))
                        .map(p -> DestinationDetailDto.MapPoiDto.builder()
                                .id(p.getId()).category(p.getCategory()).tier(p.getTier())
                                .name(p.getName()).rating(p.getRating()).priceLabel(p.getPriceLabel())
                                .imageUrl(p.getImageUrl())
                                .latitude(p.getLatitude()).longitude(p.getLongitude())
                                .build()).toList())
                .reviews(d.getReviews().stream()
                        .sorted(Comparator.comparingInt(r -> r.getSortOrder() != null ? r.getSortOrder() : 0))
                        .map(r -> DestinationDetailDto.ReviewDto.builder()
                                .id(r.getId()).authorName(r.getAuthorName())
                                .rating(r.getRating()).comment(r.getComment())
                                .build()).toList())
                .suggestedTours(tours)
                .floraSuggestion(DestinationDetailDto.FloraMatchDto.builder()
                        .destinationSlug(d.getSlug())
                        .destinationName(d.getName())
                        .matchPercent(d.getFloraMatchDefault() != null ? d.getFloraMatchDefault() : 88)
                        .matchedPreferences(List.of("Mua sắm", "Ăn uống", "Chụp ảnh"))
                        .message("Flora gợi ý: " + d.getName() + " là lựa chọn tuyệt vời cho chuyến đi Thái Lan đầu tiên.")
                        .build())
                .build();
    }

    private DestinationDetailDto.TourSuggestionDto tourToSuggestion(TourSummaryDto t) {
        String dur = "";
        if (t.getDurationDays() != null) {
            int n = t.getDurationNights() != null ? t.getDurationNights() : Math.max(0, t.getDurationDays() - 1);
            dur = t.getDurationDays() + " Ngày" + (n > 0 ? " " + n + " Đêm" : "");
        }
        return DestinationDetailDto.TourSuggestionDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .slug(t.getSlug())
                .thumbnailUrl(t.getThumbnailUrl())
                .durationLabel(dur)
                .basePrice(t.getBasePrice())
                .build();
    }

    private List<String> splitTypes(String types) {
        if (types == null || types.isBlank()) return List.of();
        return Arrays.stream(types.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}

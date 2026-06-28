package com.flourishtravel.domain.destination.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.destination.dto.AdminDestinationRequest;
import com.flourishtravel.domain.destination.dto.DestinationSummaryDto;
import com.flourishtravel.domain.destination.entity.Destination;
import com.flourishtravel.domain.destination.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminDestinationService {

    private final DestinationRepository destinationRepository;

    @Transactional(readOnly = true)
    public List<DestinationSummaryDto> listAll() {
        return destinationRepository.findAll().stream()
                .sorted(Comparator.comparing(Destination::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Destination::getName))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public DestinationSummaryDto get(UUID id) {
        return toSummary(find(id));
    }

    @Transactional
    public DestinationSummaryDto create(AdminDestinationRequest req) {
        String slug = normalizeSlug(req.getSlug());
        if (destinationRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("Slug điểm đến đã tồn tại");
        }
        Destination d = new Destination();
        d.setSlug(slug);
        apply(d, req);
        return toSummary(destinationRepository.save(d));
    }

    @Transactional
    public DestinationSummaryDto update(UUID id, AdminDestinationRequest req) {
        Destination d = find(id);
        String slug = normalizeSlug(req.getSlug());
        if (!d.getSlug().equalsIgnoreCase(slug) && destinationRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("Slug điểm đến đã tồn tại");
        }
        d.setSlug(slug);
        apply(d, req);
        return toSummary(destinationRepository.save(d));
    }

    @Transactional
    public void delete(UUID id) {
        destinationRepository.delete(find(id));
    }

    private Destination find(UUID id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", id));
    }

    private void apply(Destination d, AdminDestinationRequest req) {
        d.setName(req.getName().trim());
        d.setSummary(trim(req.getSummary()));
        d.setDescription(trim(req.getDescription()));
        d.setHeroImageUrl(trim(req.getHeroImageUrl()));
        d.setTypes(trim(req.getTypes()));
        d.setRating(req.getRating());
        d.setAvgCostMinMillion(req.getAvgCostMinMillion());
        d.setAvgCostMaxMillion(req.getAvgCostMaxMillion());
        d.setIdealDaysMin(req.getIdealDaysMin());
        d.setIdealDaysMax(req.getIdealDaysMax());
        d.setBestTimeLabel(trim(req.getBestTimeLabel()));
        d.setLocationLabel(trim(req.getLocationLabel()));
        if (req.getSortOrder() != null) d.setSortOrder(req.getSortOrder());
        if (req.getFeatured() != null) d.setFeatured(req.getFeatured());
        if (req.getPublished() != null) d.setPublished(req.getPublished());
    }

    private String normalizeSlug(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Slug không được để trống");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
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
                .idealDaysMin(d.getIdealDaysMin())
                .idealDaysMax(d.getIdealDaysMax())
                .bestTimeLabel(d.getBestTimeLabel())
                .locationLabel(d.getLocationLabel())
                .featured(d.getFeatured())
                .published(d.getPublished())
                .types(splitTypes(d.getTypes()))
                .highlightSpots(List.of())
                .build();
    }

    private List<String> splitTypes(String types) {
        if (types == null || types.isBlank()) return List.of();
        return Arrays.stream(types.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}

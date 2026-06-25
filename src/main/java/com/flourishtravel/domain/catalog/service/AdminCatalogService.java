package com.flourishtravel.domain.catalog.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.catalog.dto.AdminTravelTicketDto;
import com.flourishtravel.domain.catalog.dto.AdminTravelTicketRequest;
import com.flourishtravel.domain.catalog.entity.TravelTicket;
import com.flourishtravel.domain.catalog.repository.TravelTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final TravelTicketRepository travelTicketRepository;

    @Transactional(readOnly = true)
    public List<AdminTravelTicketDto> listAll() {
        return travelTicketRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by("name")))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminTravelTicketDto get(UUID id) {
        TravelTicket ticket = travelTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TravelTicket", id));
        return toDto(ticket);
    }

    @Transactional
    public AdminTravelTicketDto create(AdminTravelTicketRequest request) {
        String slug = normalizeRequired(request.getSlug(), "slug").toLowerCase(Locale.ROOT);
        if (travelTicketRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("slug đã tồn tại");
        }
        TravelTicket ticket = new TravelTicket();
        applyRequest(ticket, request, true);
        ticket.setSlug(slug);
        ticket.setPublished(request.getPublished() == null ? Boolean.TRUE : request.getPublished());
        ticket.setFeatured(request.getFeatured() == null ? Boolean.FALSE : request.getFeatured());
        ticket.setETicket(request.getETicket() == null ? Boolean.TRUE : request.getETicket());
        return toDto(travelTicketRepository.save(ticket));
    }

    @Transactional
    public AdminTravelTicketDto update(UUID id, AdminTravelTicketRequest request) {
        TravelTicket ticket = travelTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TravelTicket", id));
        if (request.getSlug() != null) {
            String slug = normalizeRequired(request.getSlug(), "slug").toLowerCase(Locale.ROOT);
            if (!ticket.getSlug().equalsIgnoreCase(slug) && travelTicketRepository.existsBySlugIgnoreCase(slug)) {
                throw new BadRequestException("slug đã tồn tại");
            }
            ticket.setSlug(slug);
        }
        applyRequest(ticket, request, false);
        return toDto(travelTicketRepository.save(ticket));
    }

    @Transactional
    public AdminTravelTicketDto softDelete(UUID id) {
        TravelTicket ticket = travelTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TravelTicket", id));
        ticket.setPublished(false);
        return toDto(travelTicketRepository.save(ticket));
    }

    private void applyRequest(TravelTicket ticket, AdminTravelTicketRequest request, boolean creating) {
        if (creating) {
            ticket.setName(normalizeRequired(request.getName(), "name"));
            ticket.setCategory(normalizeRequired(request.getCategory(), "category"));
        } else {
            if (request.getName() != null) ticket.setName(normalizeRequired(request.getName(), "name"));
            if (request.getCategory() != null) ticket.setCategory(normalizeRequired(request.getCategory(), "category"));
        }
        if (request.getDestinationCity() != null) ticket.setDestinationCity(normalizeNullable(request.getDestinationCity()));
        if (request.getDescription() != null) ticket.setDescription(normalizeNullable(request.getDescription()));
        if (request.getShortDescription() != null) ticket.setShortDescription(normalizeNullable(request.getShortDescription()));
        if (request.getImageUrl() != null) ticket.setImageUrl(normalizeNullable(request.getImageUrl()));
        if (request.getPriceVnd() != null) ticket.setPriceVnd(request.getPriceVnd());
        if (request.getPriceLabel() != null) ticket.setPriceLabel(normalizeNullable(request.getPriceLabel()));
        if (request.getRating() != null) ticket.setRating(request.getRating());
        if (request.getShowTimeLabel() != null) ticket.setShowTimeLabel(normalizeNullable(request.getShowTimeLabel()));
        if (request.getLocationLabel() != null) ticket.setLocationLabel(normalizeNullable(request.getLocationLabel()));
        if (request.getRouteLabel() != null) ticket.setRouteLabel(normalizeNullable(request.getRouteLabel()));
        if (request.getETicket() != null) ticket.setETicket(request.getETicket());
        if (request.getFeatured() != null) ticket.setFeatured(request.getFeatured());
        if (request.getPublished() != null) ticket.setPublished(request.getPublished());
        if (request.getSortOrder() != null) ticket.setSortOrder(request.getSortOrder());
    }

    private String normalizeNullable(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new BadRequestException(field + " không được để trống");
        }
        return normalized;
    }

    private AdminTravelTicketDto toDto(TravelTicket ticket) {
        return AdminTravelTicketDto.builder()
                .id(ticket.getId())
                .slug(ticket.getSlug())
                .name(ticket.getName())
                .category(ticket.getCategory())
                .destinationCity(ticket.getDestinationCity())
                .description(ticket.getDescription())
                .shortDescription(ticket.getShortDescription())
                .imageUrl(ticket.getImageUrl())
                .priceVnd(ticket.getPriceVnd())
                .priceLabel(ticket.getPriceLabel())
                .rating(ticket.getRating())
                .showTimeLabel(ticket.getShowTimeLabel())
                .locationLabel(ticket.getLocationLabel())
                .routeLabel(ticket.getRouteLabel())
                .eTicket(ticket.getETicket())
                .featured(ticket.getFeatured())
                .published(ticket.getPublished())
                .sortOrder(ticket.getSortOrder())
                .build();
    }
}

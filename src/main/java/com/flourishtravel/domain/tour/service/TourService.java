package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;

    @Transactional(readOnly = true)
    public Page<Tour> search(String destination, BigDecimal minPrice, BigDecimal maxPrice,
                             LocalDate startDate, UUID categoryId, Pageable pageable) {
        return tourRepository.search(destination, minPrice, maxPrice, startDate, categoryId, pageable);
    }

    @Transactional(readOnly = true)
    public Tour getById(UUID id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", id));
    }

    @Transactional(readOnly = true)
    public Tour getBySlug(String slug) {
        return tourRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", slug));
    }

    /** Tour tương tự: cùng category hoặc mới nhất, trừ tour hiện tại (AI-FEATURES: "Có thể bạn cũng thích"). */
    @Transactional(readOnly = true)
    public List<Tour> getSimilarTours(UUID tourId, int limit) {
        Tour tour = getById(tourId);
        Pageable page = PageRequest.of(0, limit);
        if (tour.getCategory() != null && tour.getCategory().getId() != null) {
            Page<Tour> byCategory = tourRepository.findByCategory_IdAndIdNotOrderByCreatedAtDesc(tour.getCategory().getId(), tourId, page);
            if (!byCategory.isEmpty()) return byCategory.getContent();
        }
        return tourRepository.findByIdNotOrderByCreatedAtDesc(tourId, page).getContent();
    }
}

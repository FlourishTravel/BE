package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.dto.AvailabilityCheckDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;

    @Transactional(readOnly = true)
    public Page<Tour> search(String destination, BigDecimal minPrice, BigDecimal maxPrice,
                             LocalDate startDate, UUID categoryId, Pageable pageable) {
        String destinationPattern = (destination != null && !destination.isBlank())
                ? "%" + destination.trim() + "%"
                : null;
        return tourRepository.search(destinationPattern, minPrice, maxPrice, startDate, categoryId, pageable);
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

    /** Kiểm tra còn chỗ: tìm session sớm nhất còn slot theo địa điểm (cho chatbot / API). */
    @Transactional(readOnly = true)
    public Optional<AvailabilityCheckDto> checkAvailability(String destination, LocalDate fromDate) {
        String destinationPattern = (destination != null && !destination.isBlank())
                ? "%" + destination.trim() + "%"
                : null;
        Page<Tour> tours = tourRepository.search(
                destinationPattern,
                null, null, fromDate != null ? fromDate : LocalDate.now(), null,
                PageRequest.of(0, 10));
        for (Tour t : tours.getContent()) {
            if (t.getSessions() == null) continue;
            for (TourSession s : t.getSessions()) {
                if (!"scheduled".equals(s.getStatus())) continue;
                if (s.getCurrentParticipants() == null || s.getMaxParticipants() == null) continue;
                if (s.getCurrentParticipants() < s.getMaxParticipants()) {
                    int remaining = s.getMaxParticipants() - s.getCurrentParticipants();
                    return Optional.of(AvailabilityCheckDto.builder()
                            .remainingSlots(remaining)
                            .nextStartDate(s.getStartDate())
                            .tourTitle(t.getTitle())
                            .tourId(t.getId())
                            .sessionId(s.getId())
                            .build());
                }
            }
        }
        return Optional.empty();
    }
}

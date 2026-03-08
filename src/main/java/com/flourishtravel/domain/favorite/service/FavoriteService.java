package com.flourishtravel.domain.favorite.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.entity.UserFavorite;
import com.flourishtravel.domain.user.repository.UserFavoriteRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;

    @Transactional(readOnly = true)
    public List<Tour> getMyFavorites(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(UserFavorite::getTour)
                .collect(Collectors.toList());
    }

    @Transactional
    public void add(UUID userId, UUID tourId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Tour tour = tourRepository.findById(tourId).orElseThrow(() -> new ResourceNotFoundException("Tour", tourId));
        if (favoriteRepository.existsByUserAndTour(user, tour)) {
            return;
        }
        favoriteRepository.save(UserFavorite.builder().user(user).tour(tour).build());
    }

    @Transactional
    public void remove(UUID userId, UUID tourId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Tour tour = tourRepository.findById(tourId).orElseThrow(() -> new ResourceNotFoundException("Tour", tourId));
        favoriteRepository.deleteByUserAndTour(user, tour);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, UUID tourId) {
        if (userId == null) return false;
        return userRepository.findById(userId).flatMap(u ->
                tourRepository.findById(tourId).map(t -> favoriteRepository.existsByUserAndTour(u, t))
        ).orElse(false);
    }
}

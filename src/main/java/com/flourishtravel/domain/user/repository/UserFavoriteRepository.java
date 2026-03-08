package com.flourishtravel.domain.user.repository;

import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UUID> {

    List<UserFavorite> findByUserOrderByCreatedAtDesc(User user);

    Optional<UserFavorite> findByUserAndTour(User user, Tour tour);

    boolean existsByUserAndTour(User user, Tour tour);

    void deleteByUserAndTour(User user, Tour tour);
}

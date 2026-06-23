package com.flourishtravel.domain.flora.repository;

import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTravelPreferenceRepository extends JpaRepository<UserTravelPreference, UUID> {

    Optional<UserTravelPreference> findByUser(User user);

    Optional<UserTravelPreference> findByUserId(UUID userId);
}

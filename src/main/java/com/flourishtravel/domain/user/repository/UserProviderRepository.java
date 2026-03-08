package com.flourishtravel.domain.user.repository;

import com.flourishtravel.domain.user.entity.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, UUID> {

    Optional<UserProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
}

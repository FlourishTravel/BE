package com.flourishtravel.domain.notification.push.repository;

import com.flourishtravel.domain.notification.push.entity.PushDevice;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceRepository extends JpaRepository<PushDevice, UUID> {

    Optional<PushDevice> findByTokenHash(String tokenHash);

    List<PushDevice> findByUserAndActiveTrue(User user);

    long countByUserAndActiveTrue(User user);

    @Modifying
    @Query("UPDATE PushDevice d SET d.active = false WHERE d.user.id = :userId AND d.tokenHash = :tokenHash")
    int deactivateByUserAndTokenHash(@Param("userId") UUID userId, @Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE PushDevice d SET d.active = false WHERE d.user.id = :userId")
    int deactivateAllForUser(@Param("userId") UUID userId);
}

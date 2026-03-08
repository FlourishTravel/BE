package com.flourishtravel.domain.chat.repository;

import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.tour.entity.TourSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findBySession(TourSession session);

    Optional<ChatRoom> findBySession_Id(UUID sessionId);
}

package com.flourishtravel.domain.chat.repository;

import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByRoomOrderByCreatedAtDesc(ChatRoom room, Pageable pageable);
}

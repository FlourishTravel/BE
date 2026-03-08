package com.flourishtravel.domain.chat.repository;

import com.flourishtravel.domain.chat.entity.Message;
import com.flourishtravel.domain.chat.entity.MessageReaction;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    Optional<MessageReaction> findByMessageAndUserAndReactionType(Message message, User user, String reactionType);
}

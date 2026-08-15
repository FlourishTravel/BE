package com.flourishtravel.domain.chat.repository;

import com.flourishtravel.domain.chat.entity.Message;
import com.flourishtravel.domain.chat.entity.MessageReaction;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    Optional<MessageReaction> findByMessageAndUserAndReactionType(Message message, User user, String reactionType);

    List<MessageReaction> findByMessageAndUser(Message message, User user);

    @EntityGraph(attributePaths = {"user", "message"})
    List<MessageReaction> findByMessage_IdIn(Collection<UUID> messageIds);
}

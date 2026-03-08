package com.flourishtravel.domain.chat.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.chat.entity.ChatMember;
import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.chat.entity.Message;
import com.flourishtravel.domain.chat.entity.MessageReaction;
import com.flourishtravel.domain.chat.repository.ChatMemberRepository;
import com.flourishtravel.domain.chat.repository.ChatRoomRepository;
import com.flourishtravel.domain.chat.repository.MessageReactionRepository;
import com.flourishtravel.domain.chat.repository.MessageRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    @Transactional(readOnly = true)
    public List<Message> getRoomMessages(UUID roomId, UUID userId, Integer limit) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("ChatRoom", roomId));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!chatMemberRepository.existsByRoomAndUser(room, user)) {
            throw new BadRequestException("Bạn không ở trong phòng chat này");
        }
        int size = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        Pageable page = PageRequest.of(0, size);
        return messageRepository.findByRoomOrderByCreatedAtDesc(room, page);
    }

    @Transactional
    public Message pinMessage(UUID messageId, UUID userId) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message", messageId));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        ensureCanModifyRoom(msg.getRoom(), user);
        if (!chatMemberRepository.existsByRoomAndUser(msg.getRoom(), user)) {
            throw new BadRequestException("Bạn không ở trong phòng chat này");
        }
        msg.setIsPinned(true);
        msg.setPinnedAt(Instant.now());
        msg.setPinnedBy(user);
        return messageRepository.save(msg);
    }

    @Transactional
    public Message unpinMessage(UUID messageId, UUID userId) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message", messageId));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        ensureCanModifyRoom(msg.getRoom(), user);
        msg.setIsPinned(false);
        msg.setPinnedAt(null);
        msg.setPinnedBy(null);
        return messageRepository.save(msg);
    }

    @Transactional
    public MessageReaction addReaction(UUID messageId, UUID userId, String reactionType) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message", messageId));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!chatMemberRepository.existsByRoomAndUser(msg.getRoom(), user)) {
            throw new BadRequestException("Bạn không ở trong phòng chat này");
        }
        String type = reactionType != null && !reactionType.isBlank() ? reactionType.trim() : "like";
        MessageReaction reaction = messageReactionRepository.findByMessageAndUserAndReactionType(msg, user, type)
                .orElseGet(() -> {
                    MessageReaction r = MessageReaction.builder()
                            .message(msg)
                            .user(user)
                            .reactionType(type)
                            .build();
                    return messageReactionRepository.save(r);
                });
        return reaction;
    }

    private void ensureCanModifyRoom(ChatRoom room, User user) {
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        if (!"ADMIN".equalsIgnoreCase(roleName) && !"TOUR_GUIDE".equalsIgnoreCase(roleName)) {
            throw new BadRequestException("Chỉ Admin hoặc Hướng dẫn viên mới được ghim/bỏ ghim tin nhắn");
        }
    }
}

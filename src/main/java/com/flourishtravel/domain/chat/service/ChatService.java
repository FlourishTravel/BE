package com.flourishtravel.domain.chat.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.chat.ChatReactionTypes;
import com.flourishtravel.domain.chat.FloraGroupChatTrigger;
import com.flourishtravel.domain.chat.TourGroupChatFloraEvent;
import com.flourishtravel.domain.chat.dto.ChatMemberViewDto;
import com.flourishtravel.domain.chat.dto.ChatMessageViewDto;
import com.flourishtravel.domain.chat.dto.ChatReactionSummaryDto;
import com.flourishtravel.domain.chat.dto.ChatReplyPreviewDto;
import com.flourishtravel.domain.chat.dto.TourChatContextDto;
import com.flourishtravel.domain.chat.entity.ChatMember;
import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.chat.entity.Message;
import com.flourishtravel.domain.chat.entity.MessageReaction;
import com.flourishtravel.domain.chat.repository.ChatMemberRepository;
import com.flourishtravel.domain.chat.repository.ChatRoomRepository;
import com.flourishtravel.domain.chat.repository.MessageReactionRepository;
import com.flourishtravel.domain.chat.repository.MessageRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TourGroupFloraService tourGroupFloraService;

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int REPLY_PREVIEW_MAX = 140;
    private static final Set<String> CHAT_ELIGIBLE_STATUSES = Set.of("paid", "confirmed", "completed");

    public boolean isChatEligibleBookingStatus(String bookingStatus) {
        if (bookingStatus == null || bookingStatus.isBlank()) {
            return false;
        }
        return CHAT_ELIGIBLE_STATUSES.contains(bookingStatus.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Tạo phòng chat cho lịch khởi hành nếu chưa có (session seed / tạo trước khi có tính năng room).
     * Trùng tuyến trình: bắt vi phạm unique {@code session_id} và đọc lại bản ghi đã tạo.
     */
    private ChatRoom ensureChatRoomForSession(TourSession session) {
        return chatRoomRepository.findBySession_Id(session.getId()).orElseGet(() -> {
            Tour tour = session.getTour();
            String title = tour != null && tour.getTitle() != null && !tour.getTitle().isBlank()
                    ? tour.getTitle()
                    : "Tour";
            String roomName = title + " - " + session.getStartDate();
            ChatRoom room = ChatRoom.builder()
                    .session(session)
                    .roomName(roomName)
                    .isActive(true)
                    .build();
            try {
                room = chatRoomRepository.save(room);
            } catch (DataIntegrityViolationException ex) {
                return chatRoomRepository.findBySession_Id(session.getId())
                        .orElseThrow(() -> ex);
            }
            User guide = session.getTourGuide();
            if (guide != null && !chatMemberRepository.existsByRoomAndUser(room, guide)) {
                chatMemberRepository.save(ChatMember.builder()
                        .room(room)
                        .user(guide)
                        .joinedAt(Instant.now())
                        .build());
            }
            return room;
        });
    }

    /**
     * Đảm bảo có phòng chat và thêm khách vào phòng khi đơn đủ điều kiện.
     * Gọi từ IPN thanh toán hoặc khi user mở màn chat / tin nhắn.
     */
    @Transactional
    public void ensureTravelerInChatRoom(Booking booking) {
        if (booking == null || booking.getSession() == null || booking.getUser() == null) {
            return;
        }
        if (!isChatEligibleBookingStatus(booking.getStatus())) {
            return;
        }
        ChatRoom room = chatRoomRepository.findBySession_Id(booking.getSession().getId())
                .orElse(null);
        if (room == null) {
            room = ensureChatRoomForSession(booking.getSession());
        }
        if (!chatMemberRepository.existsByRoomAndUser(room, booking.getUser())) {
            chatMemberRepository.save(ChatMember.builder()
                    .room(room)
                    .user(booking.getUser())
                    .joinedAt(Instant.now())
                    .build());
        }
    }

    /**
     * Khách đặt tour, HDV phụ trách session, hoặc admin.
     */
    private User resolveBookingChatActor(Booking booking, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (booking.getUser().getId().equals(userId)) {
            return user;
        }
        TourSession session = booking.getSession();
        User guide = session != null ? session.getTourGuide() : null;
        if (guide != null && guide.getId().equals(userId)) {
            return user;
        }
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            return user;
        }
        throw new ResourceNotFoundException("Booking", booking.getId());
    }

    private void ensureGuideInChatRoom(TourSession session, ChatRoom room) {
        if (session == null || room == null) {
            return;
        }
        User guide = session.getTourGuide();
        if (guide != null && !chatMemberRepository.existsByRoomAndUser(room, guide)) {
            chatMemberRepository.save(ChatMember.builder()
                    .room(room)
                    .user(guide)
                    .joinedAt(Instant.now())
                    .build());
        }
    }

    @Transactional
    public TourChatContextDto getTourChatContext(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findByIdWithSessionTourForChat(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        User actor = resolveBookingChatActor(booking, userId);
        TourSession session = booking.getSession();
        var tour = session.getTour();
        String tourTitle = tour != null ? tour.getTitle() : null;

        boolean eligible = isChatEligibleBookingStatus(booking.getStatus());
        if (eligible) {
            ensureTravelerInChatRoom(booking);
        }
        ChatRoom room = chatRoomRepository.findBySession_Id(session.getId()).orElse(null);
        if (eligible && room == null) {
            room = ensureChatRoomForSession(session);
        }
        if (eligible && room != null) {
            ensureGuideInChatRoom(session, room);
            tourGroupFloraService.ensureFloraInRoom(room);
        }

        boolean isMember = room != null && chatMemberRepository.existsByRoomAndUser(room, actor);
        boolean canChat = eligible && room != null && isMember;

        String denyReason = null;
        if (!eligible) {
            denyReason = "Chỉ mở chat sau khi đặt tour thành công (đã thanh toán / đã xác nhận).";
        } else if (room == null) {
            denyReason = "Không thể tạo phòng chat cho lịch này. Vui lòng thử lại hoặc liên hệ hỗ trợ.";
        } else if (!isMember) {
            denyReason = "Bạn chưa được thêm vào phòng chat.";
        }

        User guide = session.getTourGuide();
        String guideName = guide != null ? guide.getFullName() : null;
        String guideAvatarUrl = guide != null ? guide.getAvatarUrl() : null;

        return TourChatContextDto.builder()
                .bookingId(booking.getId())
                .sessionId(session.getId())
                .roomId(room != null ? room.getId() : null)
                .roomName(room != null ? room.getRoomName() : null)
                .tourTitle(tourTitle)
                .sessionStartDate(session.getStartDate())
                .sessionEndDate(session.getEndDate())
                .bookingStatus(booking.getStatus())
                .guideName(guideName)
                .guideAvatarUrl(guideAvatarUrl)
                .canChat(canChat)
                .denyReason(denyReason)
                .members(room != null ? toMemberViewDtos(room) : List.of())
                .build();
    }

    @Transactional
    public List<ChatMessageViewDto> getBookingChatMessages(UUID bookingId, UUID userId, Integer limit) {
        Booking booking = bookingRepository.findByIdWithSessionTourForChat(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        User actor = resolveBookingChatActor(booking, userId);
        if (!isChatEligibleBookingStatus(booking.getStatus())) {
            throw new BadRequestException("Đơn chưa đủ điều kiện để xem phòng chat.");
        }
        ensureTravelerInChatRoom(booking);
        ChatRoom room = chatRoomRepository.findBySession_Id(booking.getSession().getId())
                .orElseGet(() -> ensureChatRoomForSession(booking.getSession()));
        ensureGuideInChatRoom(booking.getSession(), room);
        tourGroupFloraService.ensureFloraInRoom(room);
        if (!chatMemberRepository.existsByRoomAndUser(room, actor)) {
            throw new BadRequestException("Bạn chưa tham gia phòng chat này.");
        }
        int size = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        Pageable page = PageRequest.of(0, size);
        List<Message> desc = messageRepository.findByRoomOrderByCreatedAtDesc(room, page);
        List<Message> chronological = new ArrayList<>(desc);
        Collections.reverse(chronological);
        return toMessageViewDtos(chronological, actor.getId());
    }

    @Transactional
    public ChatMessageViewDto sendBookingChatMessage(UUID bookingId, UUID userId, String content, UUID replyToMessageId) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Nội dung tin nhắn không được trống.");
        }
        if (trimmed.length() > 5000) {
            throw new BadRequestException("Tin nhắn quá dài (tối đa 5000 ký tự).");
        }
        Booking booking = bookingRepository.findByIdWithSessionTourForChat(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        User actor = resolveBookingChatActor(booking, userId);
        if (!isChatEligibleBookingStatus(booking.getStatus())) {
            throw new BadRequestException("Đơn chưa đủ điều kiện để gửi tin nhắn.");
        }
        ensureTravelerInChatRoom(booking);
        ChatRoom room = chatRoomRepository.findBySession_Id(booking.getSession().getId())
                .orElseGet(() -> ensureChatRoomForSession(booking.getSession()));
        ensureGuideInChatRoom(booking.getSession(), room);
        tourGroupFloraService.ensureFloraInRoom(room);
        if (!chatMemberRepository.existsByRoomAndUser(room, actor)) {
            throw new BadRequestException("Bạn chưa tham gia phòng chat này.");
        }
        Message replyTo = resolveReplyTarget(room, replyToMessageId);
        Message msg = Message.builder()
                .room(room)
                .sender(actor)
                .replyTo(replyTo)
                .messageType("text")
                .content(trimmed)
                .build();
        msg = messageRepository.save(msg);
        if (!FloraGroupChatTrigger.isFloraEmail(actor.getEmail())) {
            eventPublisher.publishEvent(new TourGroupChatFloraEvent(bookingId, userId, trimmed));
        }
        return toMessageViewDto(msg, actor.getId(), List.of());
    }

    @Transactional
    public ChatMessageViewDto toggleReaction(UUID messageId, UUID userId, String reactionType) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!chatMemberRepository.existsByRoomAndUser(msg.getRoom(), user)) {
            throw new BadRequestException("Bạn không ở trong phòng chat này");
        }
        String type = ChatReactionTypes.normalize(reactionType);
        List<MessageReaction> mine = messageReactionRepository.findByMessageAndUser(msg, user);
        boolean sameAlready = mine.stream().anyMatch(r -> type.equals(r.getReactionType()));
        if (!mine.isEmpty()) {
            messageReactionRepository.deleteAll(mine);
            messageReactionRepository.flush();
        }
        if (!sameAlready) {
            messageReactionRepository.save(MessageReaction.builder()
                    .message(msg)
                    .user(user)
                    .reactionType(type)
                    .build());
        }
        List<MessageReaction> all = messageReactionRepository.findByMessage_IdIn(List.of(msg.getId()));
        return toMessageViewDto(msg, userId, all);
    }

    private Message resolveReplyTarget(ChatRoom room, UUID replyToMessageId) {
        if (replyToMessageId == null) {
            return null;
        }
        Message replyTo = messageRepository.findById(replyToMessageId)
                .orElseThrow(() -> new BadRequestException("Tin nhắn được trả lời không còn tồn tại."));
        if (replyTo.getRoom() == null || !replyTo.getRoom().getId().equals(room.getId())) {
            throw new BadRequestException("Chỉ được trả lời tin nhắn trong cùng phòng chat.");
        }
        return replyTo;
    }

    private List<ChatMemberViewDto> toMemberViewDtos(ChatRoom room) {
        Map<UUID, ChatMemberViewDto> unique = new LinkedHashMap<>();
        for (ChatMember member : chatMemberRepository.findByRoom(room)) {
            User u = member.getUser();
            if (u == null || u.getId() == null) {
                continue;
            }
            unique.putIfAbsent(u.getId(), toMemberViewDto(u));
        }
        List<ChatMemberViewDto> list = new ArrayList<>(unique.values());
        list.sort(Comparator
                .comparing((ChatMemberViewDto m) -> !m.isFlora())
                .thenComparing(m -> roleRank(m.getRole()))
                .thenComparing(m -> m.getFullName() == null ? "" : m.getFullName(), String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private ChatMemberViewDto toMemberViewDto(User user) {
        boolean flora = FloraGroupChatTrigger.isFloraEmail(user.getEmail());
        String roleName = user.getRole() != null ? user.getRole().getName() : "TRAVELER";
        if (flora) {
            roleName = FloraGroupChatTrigger.FLORA_ROLE;
        }
        return ChatMemberViewDto.builder()
                .userId(user.getId())
                .fullName(flora ? FloraGroupChatTrigger.FLORA_NAME : user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(roleName)
                .flora(flora)
                .build();
    }

    private static int roleRank(String role) {
        if (role == null) {
            return 9;
        }
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "FLORA" -> 0;
            case "TOUR_GUIDE" -> 1;
            case "ADMIN" -> 2;
            default -> 3;
        };
    }

    private List<ChatMessageViewDto> toMessageViewDtos(List<Message> messages, UUID currentUserId) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = messages.stream().map(Message::getId).toList();
        Map<UUID, List<MessageReaction>> byMessage = messageReactionRepository.findByMessage_IdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getMessage().getId()));
        return messages.stream()
                .map(m -> toMessageViewDto(m, currentUserId, byMessage.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    private ChatMessageViewDto toMessageViewDto(Message msg, UUID currentUserId, List<MessageReaction> reactions) {
        User s = msg.getSender();
        String roleName = s.getRole() != null ? s.getRole().getName() : "TRAVELER";
        if (FloraGroupChatTrigger.isFloraEmail(s.getEmail())) {
            roleName = FloraGroupChatTrigger.FLORA_ROLE;
        }
        return ChatMessageViewDto.builder()
                .id(msg.getId())
                .content(msg.getContent())
                .messageType(msg.getMessageType())
                .createdAt(msg.getCreatedAt())
                .senderId(s.getId())
                .senderName(s.getFullName())
                .senderAvatarUrl(s.getAvatarUrl())
                .senderRole(roleName)
                .isPinned(msg.getIsPinned())
                .replyTo(toReplyPreview(msg.getReplyTo()))
                .reactions(toReactionSummaries(reactions, currentUserId))
                .build();
    }

    private ChatReplyPreviewDto toReplyPreview(Message replyTo) {
        if (replyTo == null) {
            return null;
        }
        User rs = replyTo.getSender();
        String preview = replyTo.getContent() == null ? "" : replyTo.getContent().trim();
        if (preview.length() > REPLY_PREVIEW_MAX) {
            preview = preview.substring(0, REPLY_PREVIEW_MAX - 3) + "...";
        }
        return ChatReplyPreviewDto.builder()
                .id(replyTo.getId())
                .senderId(rs != null ? rs.getId() : null)
                .senderName(rs != null ? rs.getFullName() : null)
                .content(preview)
                .build();
    }

    private List<ChatReactionSummaryDto> toReactionSummaries(List<MessageReaction> reactions, UUID currentUserId) {
        if (reactions == null || reactions.isEmpty()) {
            return List.of();
        }
        Map<String, List<MessageReaction>> grouped = reactions.stream()
                .filter(r -> r.getReactionType() != null && !r.getReactionType().isBlank())
                .collect(Collectors.groupingBy(MessageReaction::getReactionType, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(e -> ChatReactionSummaryDto.builder()
                        .type(e.getKey())
                        .count(e.getValue().size())
                        .reactedByMe(currentUserId != null && e.getValue().stream().anyMatch(r ->
                                r.getUser() != null && currentUserId.equals(r.getUser().getId())))
                        .build())
                .toList();
    }

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

    private void ensureCanModifyRoom(ChatRoom room, User user) {
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        if (!"ADMIN".equalsIgnoreCase(roleName) && !"TOUR_GUIDE".equalsIgnoreCase(roleName)) {
            throw new BadRequestException("Chỉ Admin hoặc Hướng dẫn viên mới được ghim/bỏ ghim tin nhắn");
        }
    }
}

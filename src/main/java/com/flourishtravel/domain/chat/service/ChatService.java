package com.flourishtravel.domain.chat.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.chat.dto.ChatMessageViewDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
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

    @Transactional
    public TourChatContextDto getTourChatContext(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findByIdWithSessionTourForChat(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        TourSession session = booking.getSession();
        var tour = session.getTour();
        String tourTitle = tour != null ? tour.getTitle() : null;

        boolean eligible = isChatEligibleBookingStatus(booking.getStatus());
        if (eligible) {
            ensureTravelerInChatRoom(booking);
        }
        ChatRoom room = chatRoomRepository.findBySession_Id(session.getId()).orElse(null);

        boolean isMember = room != null && chatMemberRepository.existsByRoomAndUser(room, booking.getUser());
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
                .canChat(canChat)
                .denyReason(denyReason)
                .build();
    }

    @Transactional
    public List<ChatMessageViewDto> getBookingChatMessages(UUID bookingId, UUID userId, Integer limit) {
        Booking booking = bookingRepository.findByIdWithSessionTourForChat(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        if (!isChatEligibleBookingStatus(booking.getStatus())) {
            throw new BadRequestException("Đơn chưa đủ điều kiện để xem phòng chat.");
        }
        ensureTravelerInChatRoom(booking);
        ChatRoom room = chatRoomRepository.findBySession_Id(booking.getSession().getId())
                .orElseThrow(() -> new BadRequestException("Không thể mở phòng chat cho lịch này."));
        if (!chatMemberRepository.existsByRoomAndUser(room, booking.getUser())) {
            throw new BadRequestException("Bạn chưa tham gia phòng chat này.");
        }
        int size = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        Pageable page = PageRequest.of(0, size);
        List<Message> desc = messageRepository.findByRoomOrderByCreatedAtDesc(room, page);
        List<Message> chronological = new ArrayList<>(desc);
        Collections.reverse(chronological);
        return chronological.stream().map(this::toMessageViewDto).toList();
    }

    @Transactional
    public ChatMessageViewDto sendBookingChatMessage(UUID bookingId, UUID userId, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Nội dung tin nhắn không được trống.");
        }
        if (trimmed.length() > 5000) {
            throw new BadRequestException("Tin nhắn quá dài (tối đa 5000 ký tự).");
        }
        Booking booking = bookingRepository.findByIdWithSessionTourForChat(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        if (!isChatEligibleBookingStatus(booking.getStatus())) {
            throw new BadRequestException("Đơn chưa đủ điều kiện để gửi tin nhắn.");
        }
        ensureTravelerInChatRoom(booking);
        ChatRoom room = chatRoomRepository.findBySession_Id(booking.getSession().getId())
                .orElseThrow(() -> new BadRequestException("Không thể mở phòng chat cho lịch này."));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!chatMemberRepository.existsByRoomAndUser(room, user)) {
            throw new BadRequestException("Bạn chưa tham gia phòng chat này.");
        }
        Message msg = Message.builder()
                .room(room)
                .sender(user)
                .messageType("text")
                .content(trimmed)
                .build();
        msg = messageRepository.save(msg);
        return toMessageViewDto(msg);
    }

    private ChatMessageViewDto toMessageViewDto(Message msg) {
        User s = msg.getSender();
        String roleName = s.getRole() != null ? s.getRole().getName() : "TRAVELER";
        return ChatMessageViewDto.builder()
                .id(msg.getId())
                .content(msg.getContent())
                .messageType(msg.getMessageType())
                .createdAt(msg.getCreatedAt())
                .senderId(s.getId())
                .senderName(s.getFullName())
                .senderRole(roleName)
                .isPinned(msg.getIsPinned())
                .build();
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

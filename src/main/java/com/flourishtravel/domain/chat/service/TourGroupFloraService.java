package com.flourishtravel.domain.chat.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.chat.FloraGroupChatTrigger;
import com.flourishtravel.domain.chat.TourGroupChatFloraEvent;
import com.flourishtravel.domain.chat.entity.ChatMember;
import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.chat.entity.Message;
import com.flourishtravel.domain.chat.repository.ChatMemberRepository;
import com.flourishtravel.domain.chat.repository.ChatRoomRepository;
import com.flourishtravel.domain.chat.repository.MessageRepository;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.service.ChatbotService;
import com.flourishtravel.domain.flora.recommendation.FloraNearbyChatbotHelper;
import com.flourishtravel.domain.flora.service.FloraContextBuilder;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.RoleRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Đưa Flora vào phòng chat đoàn, dùng đúng pipeline chatbot hiện có (FAQ, sự cố, LLM).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TourGroupFloraService {

    private final ChatbotService chatbotService;
    private final FloraContextBuilder floraContextBuilder;
    private final FloraNearbyChatbotHelper nearbyChatbotHelper;
    private final BookingRepository bookingRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void maybeReply(TourGroupChatFloraEvent event) {
        if (event == null || !FloraGroupChatTrigger.shouldReply(event.content())) {
            return;
        }
        try {
            replyInRoom(event.bookingId(), event.askerUserId(), event.content());
        } catch (Exception e) {
            log.warn("Flora group-chat reply failed bookingId={}", event.bookingId(), e);
        }
    }

    private void replyInRoom(UUID bookingId, UUID askerUserId, String rawContent) {
        Booking booking = bookingRepository.findByIdWithSessionTourForChat(bookingId).orElse(null);
        if (booking == null || booking.getSession() == null) {
            return;
        }
        ChatRoom room = chatRoomRepository.findBySession_Id(booking.getSession().getId()).orElse(null);
        if (room == null) {
            return;
        }

        User flora = ensureFloraUser();
        if (!chatMemberRepository.existsByRoomAndUser(room, flora)) {
            chatMemberRepository.save(ChatMember.builder()
                    .room(room)
                    .user(flora)
                    .joinedAt(java.time.Instant.now())
                    .build());
        }

        String question = FloraGroupChatTrigger.stripMention(rawContent);
        if (question.isBlank()) {
            question = "Bạn có thể giúp đoàn với các câu hỏi về lịch trình, sự cố hoặc chính sách tour không?";
        }

        TourSession session = booking.getSession();
        Tour tour = session.getTour();
        String tourTitle = tour != null ? tour.getTitle() : "tour";
        if (session.getStartDate() != null) {
            question = question + " (tour đang chat: " + tourTitle + ", khởi hành " + session.getStartDate() + ")";
        }

        ChatbotRequest request = new ChatbotRequest();
        request.setContent(question);
        request.setBookingId(bookingId);
        request.setSource("tour_group_chat");
        if (askerUserId != null) {
            request.setUserId(askerUserId.toString());
        }

        ChatbotResponse response = chatbotService.processMessage(request, askerUserId);
        response = floraContextBuilder.enrich(response, request, askerUserId);
        response = nearbyChatbotHelper.maybeEnhance(request, response, askerUserId);
        String text = formatReply(response);
        if (text.isBlank()) {
            return;
        }
        if (text.length() > 5000) {
            text = text.substring(0, 4997) + "...";
        }

        messageRepository.save(Message.builder()
                .room(room)
                .sender(flora)
                .messageType("flora")
                .content(text)
                .build());
    }

    public synchronized User ensureFloraUser() {
        return userRepository.findByEmail(FloraGroupChatTrigger.FLORA_EMAIL).orElseGet(() -> {
            Role role = roleRepository.findByName(FloraGroupChatTrigger.FLORA_ROLE)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(FloraGroupChatTrigger.FLORA_ROLE)
                            .description("Trợ lý AI Flora trong chat đoàn")
                            .build()));
            User user = User.builder()
                    .email(FloraGroupChatTrigger.FLORA_EMAIL)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()))
                    .fullName(FloraGroupChatTrigger.FLORA_NAME)
                    .jobTitle("Trợ lý AI")
                    .department("FLORA")
                    .employmentStatus("active")
                    .role(role)
                    .isActive(true)
                    .marketingOptIn(false)
                    .build();
            try {
                return userRepository.save(user);
            } catch (DataIntegrityViolationException ex) {
                return userRepository.findByEmail(FloraGroupChatTrigger.FLORA_EMAIL).orElseThrow(() -> ex);
            }
        });
    }

    static String formatReply(ChatbotResponse response) {
        if (response == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (response.getReply() != null && !response.getReply().isBlank()) {
            sb.append(response.getReply().trim());
        } else if (response.getAnswer() != null && !response.getAnswer().isBlank()) {
            sb.append(response.getAnswer().trim());
        }
        if (response.getTours() != null && !response.getTours().isEmpty()) {
            for (ChatbotResponse.TourCard tour : response.getTours()) {
                if (tour.getTitle() == null || tour.getTitle().isBlank()) {
                    continue;
                }
                sb.append("\n• ").append(tour.getTitle());
            }
        }
        return sb.toString().trim();
    }
}

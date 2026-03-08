package com.flourishtravel.domain.payment.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.chat.entity.ChatMember;
import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.chat.repository.ChatMemberRepository;
import com.flourishtravel.domain.chat.repository.ChatRoomRepository;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.repository.NotificationRepository;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoIpnService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TourSessionRepository sessionRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final NotificationRepository notificationRepository;

    @Value("${app.momo.secret-key:}")
    private String secretKey;

    @Transactional
    public void processIpn(Map<String, Object> payload) {
        String orderId = (String) payload.get("orderId");
        if (orderId == null) {
            orderId = (String) payload.get("orderId");
        }
        Object resultCodeObj = payload.get("resultCode");
        int resultCode = resultCodeObj instanceof Number ? ((Number) resultCodeObj).intValue() : 0;
        String transId = (String) payload.get("transId");
        String amountStr = payload.get("amount") != null ? payload.get("amount").toString() : null;

        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.warn("MoMo IPN: orderId not found: {}", orderId);
            return;
        }
        Payment payment = paymentOpt.get();
        Booking booking = payment.getBooking();

        if (resultCode == 0) {
            payment.setStatus("success");
            payment.setProviderTransId(transId);
            paymentRepository.save(payment);

            booking.setStatus("paid");
            bookingRepository.save(booking);

            ChatRoom room = chatRoomRepository.findBySession_Id(booking.getSession().getId()).orElse(null);
            if (room != null && !chatMemberRepository.existsByRoomAndUser(room, booking.getUser())) {
                ChatMember member = ChatMember.builder()
                        .room(room)
                        .user(booking.getUser())
                        .joinedAt(Instant.now())
                        .build();
                chatMemberRepository.save(member);
            }

            Notification notif = Notification.builder()
                    .user(booking.getUser())
                    .type("booking_confirmed")
                    .title("Đặt tour thành công")
                    .body("Bạn đã thanh toán thành công cho tour " + booking.getSession().getTour().getTitle())
                    .data("{\"booking_id\":\"" + booking.getId() + "\"}")
                    .isRead(false)
                    .build();
            notificationRepository.save(notif);
        } else {
            payment.setStatus("failed");
            paymentRepository.save(payment);
            booking.setStatus("cancelled");
            var session = booking.getSession();
            session.setCurrentParticipants(Math.max(0, session.getCurrentParticipants() - booking.getGuestCount()));
            sessionRepository.save(session);
            bookingRepository.save(booking);
        }
    }
}

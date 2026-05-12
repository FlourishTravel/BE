package com.flourishtravel.domain.payment.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.booking.service.SessionParticipantSyncService;
import com.flourishtravel.domain.chat.service.ChatService;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.repository.NotificationRepository;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Ghi nhận thanh toán MoMo thành công / thất bại (dùng chung cho IPN và đồng bộ sau redirect).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MomoPaymentCompletionService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SessionParticipantSyncService sessionParticipantSyncService;
    private final TourSessionRepository sessionRepository;
    private final ChatService chatService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void applyPaidByOrderId(String orderId, String transId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.warn("MoMo complete: orderId not found: {}", orderId);
            return;
        }
        Payment payment = paymentOpt.get();
        if ("paid".equalsIgnoreCase(payment.getStatus()) || "success".equalsIgnoreCase(payment.getStatus())) {
            return;
        }
        if (!"pending".equalsIgnoreCase(payment.getStatus())) {
            return;
        }

        Booking booking = payment.getBooking();
        payment.setStatus("paid");
        payment.setProviderTransId(transId);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        booking.setStatus("paid");
        bookingRepository.save(booking);
        sessionParticipantSyncService.syncPaidBooking(booking.getId());

        chatService.ensureTravelerInChatRoom(booking);

        String tourTitle = booking.getSession() != null && booking.getSession().getTour() != null
                ? booking.getSession().getTour().getTitle()
                : "tour";
        Notification notif = Notification.builder()
                .user(booking.getUser())
                .type("booking_confirmed")
                .title("Đặt tour thành công")
                .body("Bạn đã thanh toán thành công cho tour " + tourTitle)
                .data("{\"booking_id\":\"" + booking.getId() + "\"}")
                .isRead(false)
                .build();
        notificationRepository.save(notif);
    }

    @Transactional
    public void applyFailedByOrderId(String orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.warn("MoMo fail: orderId not found: {}", orderId);
            return;
        }
        Payment payment = paymentOpt.get();
        Booking booking = payment.getBooking();

        payment.setStatus("failed");
        paymentRepository.save(payment);

        booking.setStatus("cancelled");
        var session = booking.getSession();
        session.setCurrentParticipants(Math.max(0, session.getCurrentParticipants() - booking.getGuestCount()));
        sessionRepository.save(session);
        bookingRepository.save(booking);
    }
}

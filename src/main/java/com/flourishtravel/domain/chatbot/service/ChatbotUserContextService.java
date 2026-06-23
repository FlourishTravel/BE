package com.flourishtravel.domain.chatbot.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.entity.UserFavorite;
import com.flourishtravel.domain.user.repository.UserFavoriteRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tổng hợp lịch sử đặt tour / yêu thích của user để chatbot cá nhân hóa gợi ý.
 */
@Service
@RequiredArgsConstructor
public class ChatbotUserContextService {

    private static final int MAX_BOOKINGS = 5;
    private static final int MAX_FAVORITES = 5;
    private static final Set<String> ACTIVE_BOOKING_STATUSES = Set.of("paid", "confirmed", "completed", "pending");

    private final BookingRepository bookingRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> findUser(UUID userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId);
    }

    /** Gợi ý địa điểm ưu tiên từ favorite + booking gần đây (dùng khi user không nói rõ destination). */
    @Transactional(readOnly = true)
    public List<String> preferredDestinations(UUID userId) {
        if (userId == null) return List.of();
        Set<String> out = new LinkedHashSet<>();
        userRepository.findById(userId).ifPresent(user -> {
            for (UserFavorite fav : userFavoriteRepository.findByUserOrderByCreatedAtDesc(user)) {
                if (fav.getTour() != null) {
                    addDestination(out, fav.getTour());
                }
                if (out.size() >= MAX_FAVORITES) break;
            }
            for (Booking b : bookingRepository.findWithSummaryGraphByUserId(userId)) {
                if (b.getStatus() != null && !ACTIVE_BOOKING_STATUSES.contains(b.getStatus().toLowerCase())) continue;
                if (b.getSession() != null && b.getSession().getTour() != null) {
                    addDestination(out, b.getSession().getTour());
                }
                if (out.size() >= MAX_BOOKINGS + MAX_FAVORITES) break;
            }
        });
        return new ArrayList<>(out);
    }

    /** Text đưa vào prompt LLM — booking + favorites. */
    @Transactional(readOnly = true)
    public String buildProfileHint(UUID userId) {
        if (userId == null) return "";
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return "";

        User user = userOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Hồ sơ khách đang đăng nhập (ưu tiên gợi ý phù hợp, có thể nhắc tour tương tự đã đặt/thích):\n");
        sb.append("- Tên: ").append(user.getFullName() != null ? user.getFullName() : user.getEmail()).append("\n");

        List<UserFavorite> favorites = userFavoriteRepository.findByUserOrderByCreatedAtDesc(user);
        if (!favorites.isEmpty()) {
            sb.append("- Tour yêu thích: ");
            int n = 0;
            for (UserFavorite fav : favorites) {
                if (fav.getTour() == null) continue;
                if (n > 0) sb.append("; ");
                sb.append(formatTourLine(fav.getTour()));
                if (++n >= MAX_FAVORITES) break;
            }
            sb.append("\n");
        }

        List<Booking> bookings = bookingRepository.findWithSummaryGraphByUserId(userId);
        List<Booking> recent = bookings.stream()
                .filter(b -> b.getStatus() != null && ACTIVE_BOOKING_STATUSES.contains(b.getStatus().toLowerCase()))
                .limit(MAX_BOOKINGS)
                .toList();
        if (!recent.isEmpty()) {
            sb.append("- Đơn đặt gần đây: ");
            for (int i = 0; i < recent.size(); i++) {
                if (i > 0) sb.append("; ");
                Booking b = recent.get(i);
                Tour t = b.getSession() != null ? b.getSession().getTour() : null;
                if (t != null) {
                    sb.append(formatTourLine(t)).append(" (").append(b.getStatus()).append(")");
                }
            }
            sb.append("\n");
        }

        if (favorites.isEmpty() && recent.isEmpty()) {
            sb.append("- Chưa có booking/yêu thích — gợi ý tour phổ biến theo câu hỏi.\n");
        }
        return sb.toString();
    }

    private static void addDestination(Set<String> out, Tour tour) {
        if (tour.getDestinationCity() != null && !tour.getDestinationCity().isBlank()) {
            out.add(tour.getDestinationCity().trim());
        } else if (tour.getTitle() != null && !tour.getTitle().isBlank()) {
            out.add(tour.getTitle().trim());
        }
    }

    private static String formatTourLine(Tour tour) {
        String title = tour.getTitle() != null ? tour.getTitle() : "Tour";
        String dest = tour.getDestinationCity();
        if (dest != null && !dest.isBlank()) {
            return title + " (" + dest + ")";
        }
        return title;
    }
}

package com.flourishtravel.domain.guide.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.guide.dto.CreateGuideSessionExpenseRequest;
import com.flourishtravel.domain.guide.dto.GuideSessionExpenseDto;
import com.flourishtravel.domain.guide.dto.UpdateGuideExpenseStatusRequest;
import com.flourishtravel.domain.guide.entity.GuideSessionExpense;
import com.flourishtravel.domain.guide.repository.GuideSessionExpenseRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuideSessionExpenseService {

    private static final Set<String> ALLOWED_STATUS = Set.of("pending", "approved", "rejected");

    private final GuideSessionExpenseRepository expenseRepository;
    private final TourSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GuideSessionExpenseDto> listForSession(UUID sessionId, UUID guideId) {
        assertGuideOwnsSession(sessionId, guideId);
        return expenseRepository.findBySession_IdOrderByExpenseDateDescCreatedAtDesc(sessionId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public GuideSessionExpenseDto create(UUID sessionId, UUID guideId, CreateGuideSessionExpenseRequest request) {
        TourSession session = assertGuideOwnsSession(sessionId, guideId);
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("User", guideId));

        LocalDate expenseDate = request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now();

        GuideSessionExpense row = GuideSessionExpense.builder()
                .session(session)
                .guide(guide)
                .category(request.getCategory().trim())
                .description(request.getDescription().trim())
                .amount(request.getAmount())
                .status("pending")
                .expenseDate(expenseDate)
                .build();

        return toDto(expenseRepository.save(row));
    }

    @Transactional
    public void delete(UUID sessionId, UUID expenseId, UUID guideId) {
        assertGuideOwnsSession(sessionId, guideId);
        GuideSessionExpense row = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("GuideSessionExpense", expenseId));
        if (!row.getSession().getId().equals(sessionId)) {
            throw new BadRequestException("Chi phí không thuộc chuyến này");
        }
        if (!"pending".equalsIgnoreCase(row.getStatus())) {
            throw new BadRequestException("Chỉ xóa được chi phí đang chờ duyệt");
        }
        expenseRepository.delete(row);
    }

    @Transactional(readOnly = true)
    public List<GuideSessionExpenseDto> listForAdmin(String status) {
        List<GuideSessionExpense> rows;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            String normalized = normalizeStatus(status);
            rows = expenseRepository.findByStatusOrderByCreatedAtDesc(normalized);
        } else {
            rows = expenseRepository.findAllWithSessionOrderByCreatedAtDesc();
        }
        return rows.stream().map(this::toDto).toList();
    }

    @Transactional
    public GuideSessionExpenseDto updateStatus(UUID expenseId, UpdateGuideExpenseStatusRequest request) {
        GuideSessionExpense row = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("GuideSessionExpense", expenseId));
        String status = normalizeStatus(request.getStatus());
        if ("pending".equals(status)) {
            throw new BadRequestException("Không thể đặt lại trạng thái chờ duyệt");
        }
        row.setStatus(status);
        if (request.getAdminNote() != null) {
            row.setAdminNote(request.getAdminNote().trim());
        }
        return toDto(expenseRepository.save(row));
    }

    private String normalizeStatus(String status) {
        String s = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUS.contains(s)) {
            throw new BadRequestException("Trạng thái không hợp lệ: " + status);
        }
        return s;
    }

    private TourSession assertGuideOwnsSession(UUID sessionId, UUID guideId) {
        TourSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (session.getTourGuide() == null || !session.getTourGuide().getId().equals(guideId)) {
            throw new BadRequestException("Bạn không phải hướng dẫn viên của lịch này");
        }
        return session;
    }

    private GuideSessionExpenseDto toDto(GuideSessionExpense row) {
        TourSession session = row.getSession();
        var tour = session != null ? session.getTour() : null;
        return GuideSessionExpenseDto.builder()
                .id(row.getId())
                .sessionId(session != null ? session.getId() : null)
                .tourTitle(tour != null ? tour.getTitle() : null)
                .tourCode(tour != null ? tour.getSlug() : null)
                .category(row.getCategory())
                .description(row.getDescription())
                .amount(row.getAmount())
                .status(row.getStatus())
                .expenseDate(row.getExpenseDate())
                .adminNote(row.getAdminNote())
                .createdAt(row.getCreatedAt())
                .build();
    }
}

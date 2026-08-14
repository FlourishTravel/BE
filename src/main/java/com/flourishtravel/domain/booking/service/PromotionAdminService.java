package com.flourishtravel.domain.booking.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.dto.CreatePromotionRequest;
import com.flourishtravel.domain.booking.dto.GrantPromotionResultDto;
import com.flourishtravel.domain.booking.dto.PromotionAssigneeDto;
import com.flourishtravel.domain.booking.dto.PromotionDto;
import com.flourishtravel.domain.booking.dto.UpdatePromotionRequest;
import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.booking.entity.PromotionAssignment;
import com.flourishtravel.domain.booking.repository.PromotionAssignmentRepository;
import com.flourishtravel.domain.booking.repository.PromotionRepository;
import com.flourishtravel.domain.notification.service.NotificationService;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionAdminService {

    private final PromotionRepository promotionRepository;
    private final PromotionAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<PromotionDto> list() {
        Map<UUID, Long> assignedCounts = assignmentRepository.countGroupedByPromotionId().stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> row[1] instanceof Number n ? n.longValue() : 0L,
                        (a, b) -> a));
        return promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(p -> PromotionDto.from(p, false, assignedCounts.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionDto get(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
        return PromotionDto.from(promotion, false, assignmentRepository.countByPromotion_Id(id));
    }

    @Transactional
    public PromotionDto create(CreatePromotionRequest request) {
        validateDiscountType(request.getDiscountType());
        validateWindow(request.getValidFrom(), request.getValidTo());

        String code = normalizeRequired(request.getCode(), "code");
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("Mã khuyến mãi đã tồn tại");
        }

        Promotion promotion = Promotion.builder()
                .code(code)
                .name(normalizeNullable(request.getName()))
                .discountType(request.getDiscountType().trim().toLowerCase(Locale.ROOT))
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .isPublic(request.getIsPublic() == null ? Boolean.TRUE : request.getIsPublic())
                .build();
        return PromotionDto.from(promotionRepository.save(promotion), false, 0L);
    }

    @Transactional
    public PromotionDto update(UUID id, UpdatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));

        if (request.getCode() != null) {
            String code = normalizeRequired(request.getCode(), "code");
            if (!promotion.getCode().equalsIgnoreCase(code) && promotionRepository.existsByCodeIgnoreCase(code)) {
                throw new BadRequestException("Mã khuyến mãi đã tồn tại");
            }
            promotion.setCode(code);
        }
        if (request.getName() != null) {
            promotion.setName(normalizeNullable(request.getName()));
        }
        if (request.getDiscountType() != null) {
            validateDiscountType(request.getDiscountType());
            promotion.setDiscountType(request.getDiscountType().trim().toLowerCase(Locale.ROOT));
            if ("amount".equals(promotion.getDiscountType())) {
                promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
            }
        }
        if (request.getDiscountValue() != null) {
            promotion.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMinOrderAmount() != null) {
            promotion.setMinOrderAmount(request.getMinOrderAmount());
        }
        if (request.getMaxDiscountAmount() != null) {
            promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getValidFrom() != null) {
            promotion.setValidFrom(request.getValidFrom());
        }
        if (request.getValidTo() != null) {
            promotion.setValidTo(request.getValidTo());
        }
        if (promotion.getValidFrom() != null && promotion.getValidTo() != null) {
            validateWindow(promotion.getValidFrom(), promotion.getValidTo());
        }
        if (request.getUsageLimit() != null) {
            promotion.setUsageLimit(request.getUsageLimit());
        }
        if (request.getUsedCount() != null) {
            promotion.setUsedCount(request.getUsedCount());
        }
        if (request.getIsActive() != null) {
            promotion.setIsActive(request.getIsActive());
        }
        if (request.getIsPublic() != null) {
            promotion.setIsPublic(request.getIsPublic());
        }

        return PromotionDto.from(promotionRepository.save(promotion), false, assignmentRepository.countByPromotion_Id(id));
    }

    @Transactional
    public PromotionDto deactivate(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
        promotion.setIsActive(false);
        return PromotionDto.from(promotionRepository.save(promotion), false, assignmentRepository.countByPromotion_Id(id));
    }

    @Transactional(readOnly = true)
    public List<PromotionAssigneeDto> listAssignees(UUID promotionId) {
        requirePromotion(promotionId);
        return assignmentRepository.findWithUserByPromotionId(promotionId).stream()
                .map(this::toAssigneeDto)
                .toList();
    }

    @Transactional
    public GrantPromotionResultDto grant(UUID promotionId, List<UUID> userIds) {
        Promotion promotion = requirePromotion(promotionId);
        LinkedHashSet<UUID> distinct = userIds == null
                ? new LinkedHashSet<>()
                : userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinct.isEmpty()) {
            throw new BadRequestException("Chọn ít nhất một khách hàng");
        }

        int granted = 0;
        int skipped = 0;
        for (UUID userId : distinct) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                skipped++;
                continue;
            }
            if (assignmentRepository.existsByPromotion_IdAndUser_Id(promotionId, userId)) {
                skipped++;
                continue;
            }
            assignmentRepository.save(PromotionAssignment.builder()
                    .promotion(promotion)
                    .user(user)
                    .build());
            String code = promotion.getCode();
            String promoName = promotion.getName() != null && !promotion.getName().isBlank()
                    ? promotion.getName()
                    : code;
            notificationService.createFloraNotification(
                    userId,
                    "promotion",
                    "Bạn nhận được voucher " + code,
                    "Mã " + code + " (" + promoName + ") đã được tặng cho tài khoản của bạn. Vào Voucher của tôi để xem và nhập khi thanh toán.",
                    null);
            granted++;
        }

        List<PromotionAssigneeDto> assignees = assignmentRepository.findWithUserByPromotionId(promotionId).stream()
                .map(this::toAssigneeDto)
                .toList();
        return GrantPromotionResultDto.builder()
                .granted(granted)
                .skipped(skipped)
                .assignees(assignees)
                .build();
    }

    @Transactional
    public void revoke(UUID promotionId, UUID userId) {
        requirePromotion(promotionId);
        PromotionAssignment assignment = assignmentRepository.findByPromotion_IdAndUser_Id(promotionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PromotionAssignment", userId));
        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<PromotionDto> listVisibleTo(UUID userIdOrNull) {
        Instant now = Instant.now();
        List<PromotionDto> visible = new ArrayList<>();
        for (Promotion p : promotionRepository.findAll()) {
            if (!isCurrentlyValid(p, now)) {
                continue;
            }
            boolean isPublic = p.getIsPublic() == null || Boolean.TRUE.equals(p.getIsPublic());
            if (isPublic) {
                visible.add(PromotionDto.from(p, false, null));
            }
        }
        if (userIdOrNull == null) {
            return visible;
        }
        for (PromotionAssignment a : assignmentRepository.findWithPromotionByUserId(userIdOrNull)) {
            Promotion p = a.getPromotion();
            if (!isCurrentlyValid(p, now)) {
                continue;
            }
            boolean isPublic = p.getIsPublic() == null || Boolean.TRUE.equals(p.getIsPublic());
            if (isPublic) {
                continue;
            }
            visible.add(PromotionDto.from(p, true, null));
        }
        return visible;
    }

    private boolean isCurrentlyValid(Promotion p, Instant now) {
        if (!Boolean.TRUE.equals(p.getIsActive())) {
            return false;
        }
        if (p.getValidFrom() == null || p.getValidFrom().isAfter(now)) {
            return false;
        }
        return p.getValidTo() != null && p.getValidTo().isAfter(now);
    }

    private Promotion requirePromotion(UUID id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
    }

    private PromotionAssigneeDto toAssigneeDto(PromotionAssignment a) {
        User user = a.getUser();
        return PromotionAssigneeDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .grantedAt(a.getCreatedAt())
                .usedAt(a.getUsedAt())
                .build();
    }

    private void validateDiscountType(String discountType) {
        String normalized = discountType == null ? "" : discountType.trim().toLowerCase(Locale.ROOT);
        if (!"percent".equals(normalized) && !"amount".equals(normalized)) {
            throw new BadRequestException("discountType chỉ hỗ trợ percent hoặc amount");
        }
    }

    private void validateWindow(java.time.Instant from, java.time.Instant to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("validFrom phải trước hoặc bằng validTo");
        }
    }

    private String normalizeNullable(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new BadRequestException(field + " không được để trống");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}

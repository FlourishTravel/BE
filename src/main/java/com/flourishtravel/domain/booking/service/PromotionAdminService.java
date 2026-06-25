package com.flourishtravel.domain.booking.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.dto.CreatePromotionRequest;
import com.flourishtravel.domain.booking.dto.PromotionDto;
import com.flourishtravel.domain.booking.dto.UpdatePromotionRequest;
import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.booking.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionAdminService {

    private final PromotionRepository promotionRepository;

    @Transactional(readOnly = true)
    public List<PromotionDto> list() {
        return promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionDto get(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
        return toDto(promotion);
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
                .build();
        return toDto(promotionRepository.save(promotion));
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

        return toDto(promotionRepository.save(promotion));
    }

    @Transactional
    public PromotionDto deactivate(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
        promotion.setIsActive(false);
        return toDto(promotionRepository.save(promotion));
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
        return normalized;
    }

    private PromotionDto toDto(Promotion promotion) {
        return PromotionDto.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .minOrderAmount(promotion.getMinOrderAmount())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .validFrom(promotion.getValidFrom())
                .validTo(promotion.getValidTo())
                .usageLimit(promotion.getUsageLimit())
                .usedCount(promotion.getUsedCount())
                .isActive(promotion.getIsActive())
                .build();
    }
}

package com.flourishtravel.domain.booking.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.booking.dto.PromotionDto;
import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.booking.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionRepository promotionRepository;

    /** Mã khuyến mãi đang hiệu lực — hiển thị trang Voucher user. */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> listActive() {
        Instant now = Instant.now();
        List<PromotionDto> list = promotionRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .filter(p -> p.getValidFrom() != null && !p.getValidFrom().isAfter(now))
                .filter(p -> p.getValidTo() != null && p.getValidTo().isAfter(now))
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    private PromotionDto toDto(Promotion p) {
        return PromotionDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .minOrderAmount(p.getMinOrderAmount())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .validFrom(p.getValidFrom())
                .validTo(p.getValidTo())
                .usageLimit(p.getUsageLimit())
                .usedCount(p.getUsedCount())
                .isActive(p.getIsActive())
                .build();
    }
}

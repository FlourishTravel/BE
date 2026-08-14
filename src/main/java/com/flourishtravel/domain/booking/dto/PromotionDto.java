package com.flourishtravel.domain.booking.dto;

import com.flourishtravel.domain.booking.entity.Promotion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {
    private UUID id;
    private String code;
    private String name;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Instant validFrom;
    private Instant validTo;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean isActive;
    private Boolean isPublic;
    private Long assignedCount;
    /** true khi mã này được tặng riêng cho tài khoản đang xem. */
    private Boolean gifted;
    /** true khi chưa tới validFrom — hiện trên trang Voucher nhưng chưa dùng được lúc checkout. */
    private Boolean upcoming;

    public static PromotionDto from(Promotion p) {
        return from(p, false, null);
    }

    public static PromotionDto from(Promotion p, boolean gifted, Long assignedCount) {
        if (p == null) {
            return null;
        }
        Instant now = Instant.now();
        boolean upcoming = p.getValidFrom() != null && p.getValidFrom().isAfter(now);
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
                .isPublic(p.getIsPublic() == null || Boolean.TRUE.equals(p.getIsPublic()))
                .assignedCount(assignedCount)
                .gifted(gifted)
                .upcoming(upcoming)
                .build();
    }
}

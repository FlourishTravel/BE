package com.flourishtravel.domain.booking.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class UpdatePromotionRequest {
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
}

package com.flourishtravel.domain.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreatePromotionRequest {
    @NotBlank
    private String code;
    private String name;
    @NotBlank
    private String discountType;
    @NotNull
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    @NotNull
    private Instant validFrom;
    @NotNull
    private Instant validTo;
    private Integer usageLimit;
    private Boolean isActive;
}

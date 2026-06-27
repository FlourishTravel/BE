package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Một địa điểm thuộc tour (gắn theo ngày lịch trình). */
@Data
public class LocationRequest {

    @NotNull
    @Min(value = 1, message = "dayNumber phải >= 1")
    private Integer dayNumber;

    @Min(value = 0)
    private Integer visitOrder;

    @NotBlank(message = "Tên địa điểm không được để trống")
    @Size(max = 255)
    private String locationName;

    private BigDecimal latitude;

    private BigDecimal longitude;
}

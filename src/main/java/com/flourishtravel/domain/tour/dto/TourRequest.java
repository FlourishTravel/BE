package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payload tạo / cập nhật Tour từ admin.
 * - title: bắt buộc.
 * - slug: tuỳ chọn; nếu để trống, service sẽ tự sinh từ title.
 * - categoryId: tuỳ chọn (cho phép tour không gắn danh mục).
 */
@Data
public class TourRequest {

    @NotBlank(message = "Tên tour không được để trống")
    @Size(max = 255, message = "Tên tour tối đa 255 ký tự")
    private String title;

    @Size(max = 255, message = "Slug tối đa 255 ký tự")
    @Pattern(
            regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug chỉ gồm chữ thường, số và dấu '-'"
    )
    private String slug;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá phải >= 0")
    private BigDecimal basePrice;

    @Min(value = 1, message = "Số ngày phải >= 1")
    private Integer durationDays;

    @Min(value = 0, message = "Số đêm phải >= 0")
    private Integer durationNights;

    private UUID categoryId;

    /** domestic | international | school | corporate */
    @Pattern(
            regexp = "^$|^(domestic|international|school|corporate)$",
            message = "marketSegment phải là domestic, international, school hoặc corporate"
    )
    @Size(max = 30)
    private String marketSegment;

    @Size(max = 80)
    private String destinationCity;

    /** URL ảnh đại diện (thêm vào tour_images khi tạo). */
    private String thumbnailUrl;

    /** Gallery ảnh (ưu tiên hơn thumbnailUrl đơn). */
    private List<String> imageUrls;

    /** Video giới thiệu tour. */
    private List<TourVideoRequest> videos;
}

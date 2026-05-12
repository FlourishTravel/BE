package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload tạo / cập nhật Category (danh mục tour).
 * - name: bắt buộc, tối đa 100 ký tự.
 * - slug: tuỳ chọn; nếu để trống, service sẽ tự sinh từ name.
 *   Khi truyền, chỉ chấp nhận chữ thường, số và dấu '-'.
 * - description: mô tả tuỳ chọn.
 * - sortOrder: thứ tự hiển thị tuỳ chọn.
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String name;

    @Size(max = 100, message = "Slug tối đa 100 ký tự")
    @Pattern(
            regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug chỉ gồm chữ thường, số và dấu '-'"
    )
    private String slug;

    private String description;

    private Integer sortOrder;
}

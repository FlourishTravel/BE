package com.flourishtravel.domain.booking.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.common.converter.PiiEncryptionConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Thông tin từng khách trong đơn đặt tour (CCCD/CMND, ngày sinh cho bảo hiểm, vé, v.v.).
 * id_number: mã hóa khi lưu DB; API chỉ trả masked (***4 số cuối).
 */
@Entity
@Table(name = "booking_guests", indexes = @Index(columnList = "booking_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingGuest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** CCCD/CMND – mã hóa ở DB (PiiEncryptionConverter); không trả full qua API. */
    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "id_number", length = 256)
    @JsonIgnore
    private String idNumber;

    /** Ngày sinh – tour hạn chế tuổi, bảo hiểm theo tuổi. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Trả về CCCD đã che (chỉ 4 số cuối) – dùng trong API, không lộ full số. */
    @JsonProperty("maskedIdNumber")
    public String getMaskedIdNumber() {
        if (idNumber == null || idNumber.isBlank()) return null;
        String s = idNumber.trim();
        if (s.length() <= 4) return "***";
        return "***" + s.substring(s.length() - 4);
    }
}

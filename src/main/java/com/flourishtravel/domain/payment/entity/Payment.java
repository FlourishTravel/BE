package com.flourishtravel.domain.payment.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.booking.entity.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payments", indexes = {@Index(columnList = "booking_id"), @Index(columnList = "order_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "momo";

    @Column(name = "partner_code", length = 50)
    private String partnerCode;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "provider_trans_id", length = 100)
    private String providerTransId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String signature;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "pending";
}

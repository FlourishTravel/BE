package com.flourishtravel.domain.tour.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 1 hoạt động cụ thể trong 1 ngày của lịch trình tour.
 * Bao gồm các trường theo xu hướng modern itinerary (Klook, GetYourGuide, Viator):
 *  - Khung giờ + thời lượng
 *  - Loại hoạt động (sightseeing, dining, transport, experience, free-time, shopping, accommodation)
 *  - Toạ độ địa lý cho map integration
 *  - Ảnh hoạt động
 *  - Ước tính chi phí + đã bao gồm trong giá tour hay chưa
 *  - Tags (Instagrammable, Family-friendly, Eco-friendly, Photo-spot, Local cuisine...)
 */
@Entity
@Table(name = "tour_activities", indexes = @Index(columnList = "itinerary_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    @JsonIgnore
    private TourItinerary itinerary;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** SIGHTSEEING | DINING | TRANSPORT | EXPERIENCE | FREE_TIME | SHOPPING | ACCOMMODATION */
    @Column(name = "activity_type", length = 30)
    private String activityType;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cost_estimate", precision = 15, scale = 2)
    private BigDecimal costEstimate;

    /** True = chi phí đã bao gồm trong giá tour; False = khách tự trả. */
    @Column(name = "cost_included")
    @Builder.Default
    private Boolean costIncluded = true;

    /** Tags CSV: instagram, family, accessible, eco, photo, local-food, adventure... */
    @Column(length = 500)
    private String tags;
}

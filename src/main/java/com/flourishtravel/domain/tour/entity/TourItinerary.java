package com.flourishtravel.domain.tour.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tour_itineraries", indexes = @Index(columnList = "tour_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourItinerary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonIgnore
    private Tour tour;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Tóm tắt ngắn 1-2 câu (hook) — hiển thị ở card preview. */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Ảnh đại diện cho ngày — Instagram-style cover. */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /** Tên khách sạn / homestay / resort lưu trú. */
    @Column(length = 255)
    private String accommodation;

    /** Phương tiện di chuyển chính: xe limousine, máy bay, tàu hỏa, cáp treo, đi bộ... */
    @Column(length = 255)
    private String transport;

    /** CSV các bữa ăn bao gồm: BREAKFAST,LUNCH,DINNER */
    @Column(name = "meals_included", length = 100)
    private String mealsIncluded;

    /** Highlights / điểm nổi bật mỗi dòng 1 ý. */
    @Column(columnDefinition = "TEXT")
    private String highlights;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 32)
    @Builder.Default
    @JsonIgnore
    private List<TourActivity> activities = new ArrayList<>();
}

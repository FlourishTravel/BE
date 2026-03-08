package com.flourishtravel.domain.user.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.tour.entity.Tour;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_favorites", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "tour_id"),
        @Index(columnList = "user_id,tour_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavorite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;
}

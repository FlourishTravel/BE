package com.flourishtravel.domain.booking.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "promotion_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_promotion_assignment_promo_user",
                columnNames = {"promotion_id", "user_id"}
        ),
        indexes = {
                @Index(columnList = "promotion_id"),
                @Index(columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Thời điểm khách dùng mã này trên một đơn chưa hủy. Null = chưa dùng. */
    @Column(name = "used_at")
    private Instant usedAt;
}

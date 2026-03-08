package com.flourishtravel.domain.chatbot.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ghi lại lượt tìm kiếm từ chatbot (và sau này từ trang chủ) để cải thiện gợi ý.
 */
@Entity
@Table(name = "search_logs", indexes = {@Index(columnList = "user_id"), @Index(columnList = "created_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "search_query", length = 500)
    private String searchQuery;

    @Column(length = 255)
    private String destination;

    @Column(name = "min_price", precision = 15, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 15, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "result_count")
    private Integer resultCount;
}

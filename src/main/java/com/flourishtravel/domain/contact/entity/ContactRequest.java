package com.flourishtravel.domain.contact.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.tour.entity.Tour;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contact_requests", indexes = {@Index(columnList = "status"), @Index(columnList = "email")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactRequest extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "new";

    @Column(columnDefinition = "TEXT")
    private String note;
}

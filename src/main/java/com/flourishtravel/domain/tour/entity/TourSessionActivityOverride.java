package com.flourishtravel.domain.tour.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Per-session override of a template {@link TourActivity}. Does not modify the base tour itinerary.
 */
@Entity
@Table(name = "tour_session_activity_overrides", indexes = {
        @Index(columnList = "tour_session_id"),
        @Index(columnList = "tour_activity_id"),
        @Index(columnList = "publication_status")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tour_session_id", "tour_activity_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourSessionActivityOverride extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_session_id", nullable = false)
    private TourSession tourSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_activity_id", nullable = false)
    private TourActivity tourActivity;

    @Column(name = "title_override", length = 255)
    private String titleOverride;

    @Column(name = "description_override", columnDefinition = "TEXT")
    private String descriptionOverride;

    @Column(name = "start_time_override")
    private LocalTime startTimeOverride;

    @Column(name = "end_time_override")
    private LocalTime endTimeOverride;

    @Column(name = "location_name_override", length = 255)
    private String locationNameOverride;

    @Column(name = "location_address_override", length = 500)
    private String locationAddressOverride;

    @Column(name = "latitude_override", precision = 10, scale = 7)
    private BigDecimal latitudeOverride;

    @Column(name = "longitude_override", precision = 10, scale = 7)
    private BigDecimal longitudeOverride;

    @Column(name = "is_gathering_event_override")
    private Boolean isGatheringEventOverride;

    @Column(name = "gathering_event_type_override", length = 30)
    private String gatheringEventTypeOverride;

    /** CONFIRMED | ESTIMATED | UNAVAILABLE */
    @Column(name = "schedule_status", length = 20)
    private String scheduleStatus;

    /** DRAFT | PUBLISHED | CANCELLED */
    @Column(name = "publication_status", nullable = false, length = 20)
    @Builder.Default
    private String publicationStatus = "DRAFT";

    @Column(name = "operational_note", columnDefinition = "TEXT")
    private String operationalNote;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_user_id")
    private User publishedBy;
}

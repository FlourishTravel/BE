package com.flourishtravel.domain.flora.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_travel_preferences", indexes = @Index(columnList = "user_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTravelPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** CSV: beach,culture,food,photography,... */
    @Column(name = "travel_styles", length = 500)
    private String travelStyles;

    /** low | medium | high */
    @Column(name = "budget_level", length = 20)
    private String budgetLevel;

    @Column(name = "favorite_destinations", columnDefinition = "TEXT")
    private String favoriteDestinations;

    @Column(name = "favorite_foods", columnDefinition = "TEXT")
    private String favoriteFoods;

    @Column(name = "food_dislikes", columnDefinition = "TEXT")
    private String foodDislikes;

    @Column(name = "food_allergies", columnDefinition = "TEXT")
    private String foodAllergies;

    @Column(name = "preferred_activities", columnDefinition = "TEXT")
    private String preferredActivities;

    @Column(name = "avoided_activities", columnDefinition = "TEXT")
    private String avoidedActivities;

    /** relaxed | moderate | packed */
    @Column(name = "travel_pace", length = 30)
    private String travelPace;

    @Column(name = "traveling_with_children")
    private Boolean travelingWithChildren;

    @Column(name = "traveling_with_elderly")
    private Boolean travelingWithElderly;

    @Column(name = "notification_consent", nullable = false)
    @Builder.Default
    private Boolean notificationConsent = true;

    @Column(name = "location_consent", nullable = false)
    @Builder.Default
    private Boolean locationConsent = false;

    @Column(name = "personalization_consent", nullable = false)
    @Builder.Default
    private Boolean personalizationConsent = true;
}

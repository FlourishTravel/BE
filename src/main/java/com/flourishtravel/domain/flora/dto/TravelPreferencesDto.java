package com.flourishtravel.domain.flora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPreferencesDto {

    private List<String> travelStyles;
    private String budgetLevel;
    private List<String> favoriteDestinations;
    private List<String> favoriteFoods;
    private List<String> foodDislikes;
    private List<String> foodAllergies;
    private List<String> preferredActivities;
    private List<String> avoidedActivities;
    private String travelPace;
    private Boolean travelingWithChildren;
    private Boolean travelingWithElderly;
    private Boolean notificationConsent;
    private Boolean locationConsent;
    private Boolean personalizationConsent;
}

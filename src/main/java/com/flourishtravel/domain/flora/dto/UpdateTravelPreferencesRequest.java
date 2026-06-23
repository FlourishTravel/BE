package com.flourishtravel.domain.flora.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateTravelPreferencesRequest {

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

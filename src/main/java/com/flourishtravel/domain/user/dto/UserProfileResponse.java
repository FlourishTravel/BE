package com.flourishtravel.domain.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String gender;
    private String address;
    private String role;
    private String jobTitle;
    private String guideShortBio;
    private String guideBio;
    private java.util.List<String> guideLanguages;
    private java.util.List<String> guideSpecialties;
    private String guideCoverUrl;
    private Integer guideExperienceYears;
    private String guideBaseLocation;
    private java.util.List<String> guideBadges;
    private Boolean guideVerified;
    private Boolean guidePublicApproved;
    private Boolean guidePendingReview;
}

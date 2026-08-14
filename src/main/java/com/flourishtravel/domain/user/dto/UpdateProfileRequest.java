package com.flourishtravel.domain.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String fullName;
    private String phone;
    private String avatarUrl;
    private String gender;
    private String address;

    private String guideShortBio;
    private String guideBio;
    private java.util.List<String> guideLanguages;
    private java.util.List<String> guideSpecialties;
    private String guideCoverUrl;
    private Integer guideExperienceYears;
    private String guideBaseLocation;
}

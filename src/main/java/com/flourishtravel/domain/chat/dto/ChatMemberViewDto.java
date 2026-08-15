package com.flourishtravel.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemberViewDto {

    private UUID userId;
    private String fullName;
    private String avatarUrl;
    /** FLORA | TOUR_GUIDE | ADMIN | TRAVELER | ... */
    private String role;
    @JsonProperty("flora")
    private boolean flora;
}

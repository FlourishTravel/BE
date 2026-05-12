package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Thành viên đoàn (không serialize entity User — tránh lazy Role). */
@Data
@Builder
public class GuideSessionMemberDto {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
}

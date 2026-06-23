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
public class FloraFeedbackTagDto {

    private String id;
    private String label;
    private String category;
    private String suggestedPreferenceField;
    private String suggestedValue;
}

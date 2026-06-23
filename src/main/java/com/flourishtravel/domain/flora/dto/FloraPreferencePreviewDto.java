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
public class FloraPreferencePreviewDto {

    private List<FloraPreferenceChangeDto> changes;
    private TravelPreferencesDto mergedPreview;
    private UpdateTravelPreferencesRequest patchRequest;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FloraPreferenceChangeDto {
        private String field;
        private String before;
        private String after;
    }
}

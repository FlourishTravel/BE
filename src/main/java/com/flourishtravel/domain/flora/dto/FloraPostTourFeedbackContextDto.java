package com.flourishtravel.domain.flora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloraPostTourFeedbackContextDto {

    private UUID bookingId;
    private boolean eligible;
    private boolean alreadySubmitted;
    private String tourName;
    private String completedAt;
    private boolean personalizationEnabled;
    private List<FloraFeedbackTagDto> availableTags;
    private ExistingFeedback existingFeedback;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExistingFeedback {
        private Integer rating;
        private String comment;
        private List<String> feedbackTags;
    }
}

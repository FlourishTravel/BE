package com.flourishtravel.domain.flora.feedback;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.flora.dto.FloraFeedbackTagDto;
import com.flourishtravel.domain.flora.dto.FloraPostTourFeedbackContextDto;
import com.flourishtravel.domain.flora.dto.FloraPreferencePreviewDto;
import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest;
import com.flourishtravel.domain.flora.feedback.FloraPreferenceSuggestionMerger.PreferenceChange;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.flora.service.UserTravelPreferenceService;
import com.flourishtravel.domain.review.entity.Review;
import com.flourishtravel.domain.review.repository.ReviewRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloraPostTourFeedbackService {

    private static final DateTimeFormatter COMPLETED_AT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final FloraPrivacyService privacyService;
    private final FloraPostTourEligibility eligibility;
    private final ReviewRepository reviewRepository;
    private final UserTravelPreferenceService preferenceService;

    @Transactional(readOnly = true)
    public FloraPostTourFeedbackContextDto getContext(UUID bookingId, UUID userId) {
        Booking booking = privacyService.requireOwnedBooking(bookingId, userId);
        boolean eligible = eligibility.isEligible(booking);
        Optional<Review> existing = reviewRepository.findByBooking(booking);
        boolean alreadySubmitted = existing.isPresent();
        boolean personalizationEnabled = privacyService.hasPersonalizationConsent(userId);

        return FloraPostTourFeedbackContextDto.builder()
                .bookingId(bookingId)
                .eligible(eligible)
                .alreadySubmitted(alreadySubmitted)
                .tourName(resolveTourName(booking))
                .completedAt(formatCompletedAt(booking))
                .personalizationEnabled(personalizationEnabled)
                .availableTags(personalizationEnabled ? catalogTags() : List.of())
                .existingFeedback(existing.map(this::toExisting).orElse(null))
                .build();
    }

    @Transactional(readOnly = true)
    public FloraPreferencePreviewDto previewPreferences(UUID userId, List<String> selectedTagIds) {
        if (!privacyService.hasPersonalizationConsent(userId)) {
            return FloraPreferencePreviewDto.builder()
                    .changes(List.of())
                    .mergedPreview(preferenceService.getForUser(userId))
                    .patchRequest(new UpdateTravelPreferencesRequest())
                    .build();
        }
        TravelPreferencesDto current = preferenceService.getForUser(userId);
        List<PreferenceChange> changes = FloraPreferenceSuggestionMerger.describeChanges(selectedTagIds, current);
        TravelPreferencesDto merged = FloraPreferenceSuggestionMerger.mergePreview(selectedTagIds, current);
        UpdateTravelPreferencesRequest patch = FloraPreferenceSuggestionMerger.buildPatchRequest(selectedTagIds, current);

        return FloraPreferencePreviewDto.builder()
                .changes(changes.stream().map(c -> FloraPreferencePreviewDto.FloraPreferenceChangeDto.builder()
                        .field(c.field())
                        .before(c.before())
                        .after(c.after())
                        .build()).toList())
                .mergedPreview(merged)
                .patchRequest(patch)
                .build();
    }

    private FloraPostTourFeedbackContextDto.ExistingFeedback toExisting(Review review) {
        return FloraPostTourFeedbackContextDto.ExistingFeedback.builder()
                .rating(review.getRating())
                .comment(review.getComment())
                .feedbackTags(FloraFeedbackTagCatalog.splitTagIds(review.getFeedbackTags()))
                .build();
    }

    private static List<FloraFeedbackTagDto> catalogTags() {
        return FloraFeedbackTagCatalog.all().stream()
                .map(t -> FloraFeedbackTagDto.builder()
                        .id(t.getId())
                        .label(t.getLabel())
                        .category(t.getCategory().getValue())
                        .suggestedPreferenceField(t.getSuggestedPreferenceField())
                        .suggestedValue(t.getSuggestedValue())
                        .build())
                .toList();
    }

    private String resolveTourName(Booking booking) {
        TourSession session = booking.getSession();
        if (session == null) return null;
        Tour tour = session.getTour();
        return tour != null ? tour.getTitle() : null;
    }

    private String formatCompletedAt(Booking booking) {
        ZonedDateTime at = eligibility.resolveCompletedAt(booking);
        return at != null ? at.format(COMPLETED_AT_FMT) : null;
    }
}

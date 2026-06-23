package com.flourishtravel.domain.tour.schedule;

import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.tour.SessionScheduleConstants;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Merges template activity with optional published session override for Flora journey resolution.
 */
public final class SessionScheduleMergeHelper {

    private SessionScheduleMergeHelper() {}

    @Value
    @Builder
    public static class EffectiveActivityFields {
        String title;
        String description;
        LocalTime startTime;
        LocalTime endTime;
        String locationName;
        String locationAddress;
        BigDecimal latitude;
        BigDecimal longitude;
        boolean gatheringEvent;
        String gatheringEventType;
        String scheduleStatus;
        String scheduleSource;
        Integer scheduleVersion;
        Instant lastUpdatedAt;
        String lastUpdatedReason;
        boolean cancelled;
        boolean fromPublishedOverride;
    }

    public static EffectiveActivityFields resolve(
            TourActivity template,
            TourSessionActivityOverride publishedOverride) {

        if (publishedOverride != null
                && SessionScheduleConstants.PUBLICATION_CANCELLED.equalsIgnoreCase(publishedOverride.getPublicationStatus())) {
            return EffectiveActivityFields.builder()
                    .cancelled(true)
                    .scheduleSource(SessionScheduleConstants.SOURCE_UNAVAILABLE)
                    .build();
        }

        boolean fromOverride = publishedOverride != null
                && SessionScheduleConstants.PUBLICATION_PUBLISHED.equalsIgnoreCase(publishedOverride.getPublicationStatus());

        String title = pick(fromOverride ? publishedOverride.getTitleOverride() : null, template.getTitle());
        String description = pick(fromOverride ? publishedOverride.getDescriptionOverride() : null, template.getDescription());
        LocalTime start = pick(fromOverride ? publishedOverride.getStartTimeOverride() : null, template.getStartTime());
        LocalTime end = pick(fromOverride ? publishedOverride.getEndTimeOverride() : null, template.getEndTime());
        String locationName = pick(fromOverride ? publishedOverride.getLocationNameOverride() : null, template.getLocationName());
        String locationAddress = pick(fromOverride ? publishedOverride.getLocationAddressOverride() : null, template.getLocationAddress());
        BigDecimal lat = pick(fromOverride ? publishedOverride.getLatitudeOverride() : null, template.getLatitude());
        BigDecimal lon = pick(fromOverride ? publishedOverride.getLongitudeOverride() : null, template.getLongitude());
        boolean gathering = fromOverride && publishedOverride.getIsGatheringEventOverride() != null
                ? publishedOverride.getIsGatheringEventOverride()
                : Boolean.TRUE.equals(template.getIsGatheringEvent());
        String gatheringType = pick(
                fromOverride ? publishedOverride.getGatheringEventTypeOverride() : null,
                template.getGatheringEventType());

        String scheduleStatus;
        String scheduleSource;
        if (fromOverride && publishedOverride.getScheduleStatus() != null && !publishedOverride.getScheduleStatus().isBlank()) {
            scheduleStatus = publishedOverride.getScheduleStatus().trim().toUpperCase();
            scheduleSource = SessionScheduleConstants.SOURCE_SESSION_OVERRIDE;
        } else {
            scheduleStatus = resolveTemplateScheduleStatus(template);
            scheduleSource = SessionScheduleConstants.SOURCE_TOUR_TEMPLATE;
        }

        if (fromOverride && FloraScheduleConstants.SCHEDULE_CONFIRMED.equals(scheduleStatus)) {
            scheduleSource = SessionScheduleConstants.SOURCE_SESSION_OVERRIDE;
        } else if (fromOverride && FloraScheduleConstants.SCHEDULE_ESTIMATED.equals(scheduleStatus)) {
            scheduleSource = SessionScheduleConstants.SOURCE_SESSION_OVERRIDE;
        }

        return EffectiveActivityFields.builder()
                .title(title)
                .description(description)
                .startTime(start)
                .endTime(end)
                .locationName(locationName)
                .locationAddress(locationAddress)
                .latitude(lat)
                .longitude(lon)
                .gatheringEvent(gathering)
                .gatheringEventType(gatheringType)
                .scheduleStatus(scheduleStatus)
                .scheduleSource(scheduleSource)
                .scheduleVersion(fromOverride ? publishedOverride.getVersion() : null)
                .lastUpdatedAt(fromOverride ? publishedOverride.getPublishedAt() : null)
                .lastUpdatedReason(fromOverride ? publishedOverride.getOperationalNote() : null)
                .cancelled(false)
                .fromPublishedOverride(fromOverride)
                .build();
    }

    public static EffectiveActivityFields resolveDraftPreview(
            TourActivity template,
            TourSessionActivityOverride draftOverride) {
        if (draftOverride == null) {
            return resolve(template, null);
        }
        TourSessionActivityOverride preview = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_PUBLISHED)
                .titleOverride(draftOverride.getTitleOverride())
                .descriptionOverride(draftOverride.getDescriptionOverride())
                .startTimeOverride(draftOverride.getStartTimeOverride())
                .endTimeOverride(draftOverride.getEndTimeOverride())
                .locationNameOverride(draftOverride.getLocationNameOverride())
                .locationAddressOverride(draftOverride.getLocationAddressOverride())
                .latitudeOverride(draftOverride.getLatitudeOverride())
                .longitudeOverride(draftOverride.getLongitudeOverride())
                .isGatheringEventOverride(draftOverride.getIsGatheringEventOverride())
                .gatheringEventTypeOverride(draftOverride.getGatheringEventTypeOverride())
                .scheduleStatus(draftOverride.getScheduleStatus())
                .operationalNote(draftOverride.getOperationalNote())
                .version(draftOverride.getVersion())
                .publishedAt(draftOverride.getPublishedAt())
                .build();
        return resolve(template, preview);
    }

    public static boolean isPublishedForTravelers(TourSessionActivityOverride override) {
        return override != null
                && SessionScheduleConstants.PUBLICATION_PUBLISHED.equalsIgnoreCase(override.getPublicationStatus());
    }

    private static String resolveTemplateScheduleStatus(TourActivity template) {
        if (template.getScheduleStatus() != null && !template.getScheduleStatus().isBlank()) {
            return template.getScheduleStatus().trim().toUpperCase();
        }
        if (template.getStartTime() == null) {
            return FloraScheduleConstants.SCHEDULE_UNAVAILABLE;
        }
        return FloraScheduleConstants.SCHEDULE_ESTIMATED;
    }

    private static <T> T pick(T overrideValue, T templateValue) {
        if (overrideValue != null) {
            if (overrideValue instanceof String s && s.isBlank()) return templateValue;
            return overrideValue;
        }
        return templateValue;
    }
}

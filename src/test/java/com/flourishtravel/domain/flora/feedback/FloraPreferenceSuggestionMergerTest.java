package com.flourishtravel.domain.flora.feedback;

import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FloraPreferenceSuggestionMergerTest {

    @Test
    void mergePreview_appendsWithoutDuplicates() {
        TravelPreferencesDto current = TravelPreferencesDto.builder()
                .preferredActivities(new ArrayList<>(List.of("chụp ảnh")))
                .travelPace("vừa phải")
                .build();

        TravelPreferencesDto merged = FloraPreferenceSuggestionMerger.mergePreview(
                List.of("PHOTO_SPOTS", "COFFEE", "RELAXED_PACE"), current);

        assertEquals(List.of("chụp ảnh", "cà phê"), merged.getPreferredActivities());
        assertEquals("chậm", merged.getTravelPace());
    }

    @Test
    void buildPatchRequest_onlyIncludesChangedFields() {
        TravelPreferencesDto current = TravelPreferencesDto.builder()
                .preferredActivities(new ArrayList<>(List.of("đi bộ")))
                .travelPace("vừa phải")
                .favoriteFoods(List.of("phở"))
                .build();

        UpdateTravelPreferencesRequest patch = FloraPreferenceSuggestionMerger.buildPatchRequest(
                List.of("COFFEE"), current);

        assertEquals(List.of("đi bộ", "cà phê"), patch.getPreferredActivities());
        assertNull(patch.getFavoriteFoods());
        assertNull(patch.getTravelPace());
    }

    @Test
    void describeChanges_listsNewValuesOnly() {
        TravelPreferencesDto current = TravelPreferencesDto.builder()
                .avoidedActivities(List.of())
                .build();

        var changes = FloraPreferenceSuggestionMerger.describeChanges(
                List.of("DISLIKE_RUSHED"), current);

        assertEquals(1, changes.size());
        assertEquals("avoidedActivities", changes.get(0).field());
        assertEquals("lịch trình gấp", changes.get(0).after());
    }

    @Test
    void emptyTags_preservesCurrent() {
        TravelPreferencesDto current = TravelPreferencesDto.builder()
                .travelStyles(List.of("Biển"))
                .build();

        TravelPreferencesDto merged = FloraPreferenceSuggestionMerger.mergePreview(List.of(), current);
        assertEquals(List.of("Biển"), merged.getTravelStyles());
    }
}

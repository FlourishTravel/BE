package com.flourishtravel.domain.flora.feedback;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FloraGuideFeedbackTagCatalogTest {

    @Test
    void acceptsKnownGuideTags() {
        assertDoesNotThrow(() -> FloraGuideFeedbackTagCatalog.validateTagIds(
                List.of("GUIDE_ATTENTIVE", "GUIDE_ATTITUDE")));
        assertEquals("HDV tận tâm", FloraGuideFeedbackTagCatalog.find("GUIDE_ATTENTIVE").orElseThrow().label());
    }

    @Test
    void rejectsUnknownGuideTag() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FloraGuideFeedbackTagCatalog.validateTagIds(List.of("COFFEE")));
        assertTrue(ex.getMessage().contains("không hợp lệ"));
    }
}

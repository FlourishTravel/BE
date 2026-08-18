package com.flourishtravel.domain.flora.recommendation;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FloraGiftShoppingAdviceTest {

    @Test
    void parsesBigCMotherGiftInBaht() {
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse(
                "Khách đang ở Big C Ratchadamri và hỏi Flora mua quà cho mẹ trong ngân sách 500 baht");

        assertTrue(ask.giftIntent());
        assertTrue(ask.inStore());
        assertFalse(ask.wantsNearbyStores());
        assertTrue(ask.venue().toLowerCase().contains("big c"));
        assertTrue(ask.venue().toLowerCase().contains("ratchadamri"));
        assertEquals("mẹ", ask.recipient());
        assertEquals(500, ask.budgetAmount());
        assertEquals("THB", ask.budgetCurrency());
    }

    @Test
    void giftWithoutVenueAsksWhere() {
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse("Mua quà cho mẹ ngân sách 500 baht");
        assertTrue(ask.giftIntent());
        assertFalse(ask.inStore());
        assertFalse(ask.wantsNearbyStores());
        assertNull(ask.venue());
        assertEquals(500, ask.budgetAmount());
    }

    @Test
    void giftNearbyLooksForStores() {
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse("Mua quà gần đây cho mẹ 300 THB");
        assertTrue(ask.giftIntent());
        assertTrue(ask.wantsNearbyStores());
        assertFalse(ask.inStore());
        assertEquals(300, ask.budgetAmount());
    }

    @Test
    void doesNotTreatTourBudgetAsGift() {
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse("Tour biển 3 ngày tầm 5 triệu");
        assertFalse(ask.giftIntent());
        assertFalse(ask.inStore());
    }

    @Test
    void hintForbidsBahtAsMillionVnd() {
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse(
                "đang ở Big C Ratchadamri, mua quà cho mẹ 500 baht");
        String hint = FloraGiftShoppingAdvice.buildLlmHint(ask, null);
        assertTrue(hint.contains("Big C"));
        assertTrue(hint.contains("500"));
        assertTrue(hint.toLowerCase().contains("không phải triệu"));
        assertTrue(hint.contains("search_tour") || hint.contains("không search_tour"));
    }

    @Test
    void stripsTourBudgetAndCoercesIntent() {
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse("mua quà cho mẹ 500 baht");
        Map<String, Object> slots = new HashMap<>();
        slots.put("budget_min", 500);
        slots.put("budget_max", 500);
        FloraGiftShoppingAdvice.stripTourBudgetSlots(slots, ask);
        assertFalse(slots.containsKey("budget_min"));
        assertEquals("travel_tips", FloraGiftShoppingAdvice.coerceIntent("search_tour", ask));
    }

    @Test
    void knownChainWithoutDangO() {
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse(
                "Mình ở 7-Eleven Siam, chọn quà cho bạn gái 200฿");
        assertTrue(ask.giftIntent());
        assertNotNull(ask.venue());
        assertTrue(ask.venue().toLowerCase().contains("7"));
        assertEquals("bạn gái", ask.recipient());
        assertEquals(200, ask.budgetAmount());
    }
}

package com.flourishtravel.domain.notification.push;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PushTokenHasherTest {

    @Test
    void hash_isStableAndNonEmpty() {
        String h1 = PushTokenHasher.hash("sample-fcm-token-value");
        String h2 = PushTokenHasher.hash("sample-fcm-token-value");
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
        assertNotEquals("sample-fcm-token-value", h1);
    }
}

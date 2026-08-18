package com.flourishtravel.domain.booking;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Mã đặt chỗ khách thấy trên hóa đơn / vé: FT- + 8 ký tự đầu UUID. */
public final class BookingCodes {

    private BookingCodes() {
    }

    public static String fromId(UUID id) {
        if (id == null) {
            return null;
        }
        return "FT-" + id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /** Nội dung QR trên vé — trùng mã đặt chỗ, áp dụng cả đơn cũ. */
    public static String qrPayload(UUID id) {
        return fromId(id);
    }

    public static Optional<UUID> parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /** Chuẩn hóa FT-XXXXXXXX từ mã đặt chỗ hoặc UUID. */
    public static String normalize(String raw) {
        Optional<UUID> uuid = parseUuid(raw);
        if (uuid.isPresent()) {
            return fromId(uuid.get());
        }
        return codePrefix(raw)
                .map(prefix -> "FT-" + prefix.toUpperCase(Locale.ROOT))
                .orElse(null);
    }

    static Optional<String> codePrefix(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.length() < 4 || !trimmed.regionMatches(true, 0, "FT-", 0, 3)) {
            return Optional.empty();
        }
        String hex = trimmed.substring(3).replaceAll("[^a-fA-F0-9]", "");
        if (hex.length() < 8) {
            return Optional.empty();
        }
        return Optional.of(hex.substring(0, 8).toLowerCase(Locale.ROOT));
    }
}

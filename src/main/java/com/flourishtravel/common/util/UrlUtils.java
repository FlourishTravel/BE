package com.flourishtravel.common.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Ghép base (có thể kết thúc bằng /) với path — tránh {@code https://host/FE//checkout/...}.
 */
public final class UrlUtils {

    private UrlUtils() {}

    /** Gắn thêm một query param (encode UTF-8). */
    public static String appendQueryParam(String url, String name, String value) {
        if (url == null || url.isBlank() || name == null || name.isBlank() || value == null) {
            return url;
        }
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + name + "=" + encoded;
    }

    /**
     * Gộp origin/base với path bắt đầu bằng /, rồi gộp các dấu / thừa (không phá {@code https://}).
     */
    public static String joinBaseAndPath(String base, String path) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        String p = path == null ? "" : path.trim();
        if (p.isEmpty()) {
            return squashDuplicateSlashesExceptScheme(b);
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return squashDuplicateSlashesExceptScheme(b.isEmpty() ? p : b + p);
    }

    /** Ví dụ {@code https://x.com/a//b} → {@code https://x.com/a/b}. */
    public static String squashDuplicateSlashesExceptScheme(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.replaceAll("([^:])//+", "$1/");
    }
}

package com.flourishtravel.domain.user;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CsvLists {

    private CsvLists() {}

    public static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public static String join(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        String joined = items.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? null : joined;
    }
}

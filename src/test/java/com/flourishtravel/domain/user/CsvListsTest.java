package com.flourishtravel.domain.user;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvListsTest {

    @Test
    void splitAndJoin() {
        assertEquals(List.of("Ẩm thực", "Văn hóa"), CsvLists.split("Ẩm thực, Văn hóa"));
        assertEquals("Ẩm thực, Văn hóa", CsvLists.join(List.of("Ẩm thực", "Văn hóa", "Ẩm thực")));
        assertEquals(List.of(), CsvLists.split("  "));
    }
}

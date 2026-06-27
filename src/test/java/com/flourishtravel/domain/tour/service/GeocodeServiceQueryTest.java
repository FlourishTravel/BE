package com.flourishtravel.domain.tour.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeocodeServiceQueryTest {

    @Test
    void buildQueries_prefersLocationNameBeforeLongAddress() {
        List<String> queries = GeocodeService.buildQueries(
                "bến xe Cần Thơ",
                "Khu đô thị Nam Cần Thơ, Quốc lộ 1A, Phường Hưng Thạnh, Quận Cái Răng, Thành phố Cần Thơ",
                "Cần Thơ");

        assertEquals("bến xe Cần Thơ", queries.get(0));
        assertTrue(queries.stream().anyMatch(q -> q.startsWith("bến xe Cần Thơ, Khu")));
        assertTrue(queries.contains("Cần Thơ"));
    }
}

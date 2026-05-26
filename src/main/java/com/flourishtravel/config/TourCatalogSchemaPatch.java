package com.flourishtravel.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bổ sung cột catalog trên bảng tours khi DB đã tồn tại trước khi entity mở rộng
 * (tránh native query / Hibernate thiếu cột badge, destination_city, ...).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TourCatalogSchemaPatch {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Order(4)
    public void ensureTourCatalogColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS destination_city VARCHAR(80)");
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS rating NUMERIC(3,2)");
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS badge VARCHAR(40)");
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS tags VARCHAR(300)");
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS featured BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS highlights_text TEXT");
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS includes_text TEXT");
            jdbcTemplate.execute("ALTER TABLE tours ADD COLUMN IF NOT EXISTS excludes_text TEXT");
            ensureTravelTicketsTable();
        } catch (Exception e) {
            log.warn("Tour catalog schema patch skipped: {}", e.getMessage());
        }
    }

    private void ensureTravelTicketsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS travel_tickets (
                    id UUID PRIMARY KEY,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMPTZ,
                    slug VARCHAR(120) NOT NULL UNIQUE,
                    name VARCHAR(200) NOT NULL,
                    category VARCHAR(40) NOT NULL,
                    destination_city VARCHAR(80),
                    description TEXT,
                    short_description VARCHAR(500),
                    image_url VARCHAR(500),
                    price_vnd NUMERIC(15,2),
                    price_label VARCHAR(80),
                    rating NUMERIC(3,2),
                    show_time_label VARCHAR(120),
                    location_label VARCHAR(200),
                    route_label VARCHAR(200),
                    e_ticket BOOLEAN DEFAULT TRUE,
                    featured BOOLEAN DEFAULT FALSE,
                    published BOOLEAN DEFAULT TRUE,
                    sort_order INTEGER
                )
                """);
    }
}

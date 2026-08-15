package com.flourishtravel.config;

import com.flourishtravel.domain.booking.service.SessionOccupancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ghi lại số chỗ đợt khởi hành = tổng khách trên đơn chưa hủy (sửa bộ đếm đã lệch).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionOccupancyBackfill {

    private final SessionOccupancyService sessionOccupancyService;

    @EventListener(ApplicationReadyEvent.class)
    @Order(200)
    public void syncHeldSeats() {
        int updated = sessionOccupancyService.syncAll();
        if (updated > 0) {
            log.info("Đã đồng bộ số chỗ {} đợt khởi hành theo đơn đặt chưa hủy", updated);
        }
    }
}

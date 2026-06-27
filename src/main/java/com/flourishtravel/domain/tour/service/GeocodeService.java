package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.tour.client.VietMapClient;
import com.flourishtravel.domain.tour.dto.GeocodeResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodeService {

    private static final String PROVIDER = "vietmap";

    private final VietMapClient vietMapClient;

    public GeocodeResultDto resolveActivityCoordinates(
            String locationName,
            String locationAddress,
            String destinationCity) {
        if (!vietMapClient.isConfigured()) {
            throw new BadRequestException(
                    "Chưa cấu hình VietMap API key trên server (VIETMAP_API_KEY hoặc MAP_VIET_ACESS_KEY). "
                            + "Lấy key tại https://maps.vietmap.vn/console");
        }

        List<String> queries = buildQueries(locationName, locationAddress, destinationCity);
        if (queries.isEmpty()) {
            throw new BadRequestException("Nhập tên địa điểm hoặc địa chỉ trước khi lấy tọa độ.");
        }

        for (String query : queries) {
            try {
                var hit = vietMapClient.geocode(query);
                if (hit.isPresent()) {
                    var h = hit.get();
                    return GeocodeResultDto.builder()
                            .latitude(h.latitude())
                            .longitude(h.longitude())
                            .label(h.label())
                            .provider(PROVIDER)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Geocode query failed for '{}': {}", query, e.getMessage());
            }
        }

        if (vietMapClient.hadAuthError()) {
            throw new BadRequestException(
                    "VietMap từ chối API key (401/403). Kiểm tra VIETMAP_API_KEY trên server và giới hạn IP/domain trên VietMap Console.");
        }

        throw new BadRequestException(
                "Không tìm thấy tọa độ trên VietMap cho: "
                        + String.join(" | ", queries.subList(0, Math.min(queries.size(), 2)))
                        + ". Thử tên ngắn hơn hoặc nhập Lat/Lng thủ công.");
    }

    /** Ưu tiên tên địa điểm ngắn trước địa chỉ dài — VietMap hay match tốt hơn. */
    static List<String> buildQueries(String locationName, String locationAddress, String destinationCity) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        String name = trimToNull(locationName);
        String address = trimToNull(locationAddress);
        String city = trimToNull(destinationCity);

        if (name != null) {
            queries.add(name);
            if (city != null && !name.toLowerCase().contains(city.toLowerCase())) {
                queries.add(name + ", " + city);
            }
        }
        if (name != null && address != null) {
            queries.add(name + ", " + address);
        }
        if (address != null) {
            queries.add(address);
        }
        if (city != null) {
            queries.add(city);
        }
        return new ArrayList<>(queries);
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

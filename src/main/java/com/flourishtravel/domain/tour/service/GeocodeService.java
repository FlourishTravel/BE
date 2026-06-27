package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.tour.client.VietMapClient;
import com.flourishtravel.domain.tour.dto.GeocodeResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
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

        List<String> queries = new ArrayList<>();
        if (locationAddress != null && !locationAddress.isBlank()) {
            queries.add(locationAddress.trim());
        }
        if (locationName != null && !locationName.isBlank()) {
            queries.add(locationName.trim());
        }
        if (destinationCity != null && !destinationCity.isBlank()) {
            queries.add(destinationCity.trim());
        }
        if (queries.isEmpty()) {
            throw new BadRequestException("Nhập tên địa điểm hoặc địa chỉ trước khi lấy tọa độ.");
        }

        for (String query : queries) {
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
        }

        throw new BadRequestException("Không tìm thấy tọa độ trên VietMap. Thử địa chỉ chi tiết hơn hoặc nhập thủ công.");
    }
}

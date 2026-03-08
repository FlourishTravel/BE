package com.flourishtravel.domain.chatbot.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.dto.NearbyPlaceDto;
import com.flourishtravel.domain.chatbot.dto.WeatherForecastDto;
import com.flourishtravel.domain.chatbot.service.ChatbotDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API lấy data phục vụ chatbot: địa điểm gần đây, thời tiết.
 * Trùng với api_endpoint trong config để frontend/AI gọi được.
 */
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotDataController {

    private final ChatbotDataService chatbotDataService;

    /** Địa điểm gần đây (quán ăn, cafe...) — sau tích hợp Google Places. */
    @GetMapping("/nearby-places")
    public ResponseEntity<ApiResponse<NearbyPlaceDto>> nearbyPlaces(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String poi_type) {
        NearbyPlaceDto dto = chatbotDataService.getNearbyPlace(destination, poi_type);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    /** Dự báo thời tiết theo điểm đến — sau tích hợp API thời tiết. */
    @GetMapping("/weather-forecast")
    public ResponseEntity<ApiResponse<WeatherForecastDto>> weatherForecast(
            @RequestParam(required = false) String destination) {
        WeatherForecastDto dto = chatbotDataService.getWeatherForecast(destination);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}

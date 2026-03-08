package com.flourishtravel.domain.chatbot.service;

import com.flourishtravel.domain.chatbot.client.OpenMeteoClient;
import com.flourishtravel.domain.chatbot.client.OverpassClient;
import com.flourishtravel.domain.chatbot.dto.NearbyPlaceDto;
import com.flourishtravel.domain.chatbot.dto.WeatherForecastDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Cung cấp data cho chatbot: địa điểm gần đây (Overpass/OSM), thời tiết (Open-Meteo).
 * Có fallback sang dữ liệu mẫu khi API lỗi hoặc không có kết quả.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotDataService {

    private final OpenMeteoClient openMeteoClient;
    private final OverpassClient overpassClient;

    private static String toSearchName(String destination) {
        if (destination == null || destination.isBlank()) return "Vietnam";
        return destination
                .replace("Đà Nẵng", "Da Nang")
                .replace("Đà Lạt", "Da Lat")
                .replace("Phú Quốc", "Phu Quoc")
                .replace("Nha Trang", "Nha Trang")
                .replace("Hội An", "Hoi An")
                .replace("Hạ Long", "Ha Long")
                .replace("Vũng Tàu", "Vung Tau")
                .replace("Ninh Bình", "Ninh Binh")
                .replace("Hà Giang", "Ha Giang")
                .replace("Phan Thiết", "Phan Thiet")
                .replace("Côn Đảo", "Con Dao")
                .replace("Quy Nhơn", "Quy Nhon")
                .replace("Cần Thơ", "Can Tho")
                .trim() + " Vietnam";
    }

    /** Gợi ý địa điểm gần đây (Overpass/OSM). Fallback mock nếu API lỗi. */
    public NearbyPlaceDto getNearbyPlace(String destination, String poiType) {
        String dest = destination != null && !destination.isBlank() ? destination : "Đà Nẵng";
        String type = poiType != null && !poiType.isBlank() ? poiType : "quán ăn";
        double[] coords = openMeteoClient.geocode(toSearchName(dest));
        if (coords != null) {
            Map<String, Object> poi = overpassClient.findNearbyPoi(coords[0], coords[1], type);
            if (poi != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tags = (Map<String, Object>) poi.get("tags");
                String name = tags != null && tags.get("name") != null ? tags.get("name").toString() : "Quán địa phương";
                String addr = tags != null && tags.get("addr:street") != null ? tags.get("addr:street").toString() : "Gần khu trung tâm";
                return NearbyPlaceDto.builder()
                        .name(name)
                        .type(type)
                        .distance("khoảng 500m - 2km")
                        .rating(4.0)
                        .address(addr)
                        .build();
            }
        }
        return NearbyPlaceDto.builder()
                .name("Quán địa phương gợi ý")
                .type(type)
                .distance("khoảng 500m")
                .rating(4.5)
                .address("Gần khu trung tâm")
                .build();
    }

    /** Dự báo thời tiết (Open-Meteo). Fallback mock nếu API lỗi. */
    public WeatherForecastDto getWeatherForecast(String destination) {
        String dest = destination != null && !destination.isBlank() ? destination : "Đà Nẵng";
        double[] coords = openMeteoClient.geocode(toSearchName(dest));
        if (coords != null) {
            Map<String, Object> forecast = openMeteoClient.getForecast(coords[0], coords[1]);
            if (forecast != null) return mapOpenMeteoToDto(dest, forecast);
        }
        return WeatherForecastDto.builder()
                .destination(dest)
                .summary("Nắng nhẹ, chiều tối có thể mưa rào. Nhiệt độ 26-32°C.")
                .days(List.of(
                        WeatherForecastDto.DayForecast.builder().date("Hôm nay").condition("Nắng").tempMin("26°C").tempMax("32°C").build(),
                        WeatherForecastDto.DayForecast.builder().date("Ngày mai").condition("Nắng, mây").tempMin("25°C").tempMax("31°C").build(),
                        WeatherForecastDto.DayForecast.builder().date("Ngày kia").condition("Mưa rào").tempMin("24°C").tempMax("30°C").build()
                ))
                .build();
    }

    @SuppressWarnings("unchecked")
    private WeatherForecastDto mapOpenMeteoToDto(String destination, Map<String, Object> forecast) {
        StringBuilder summary = new StringBuilder();
        Object current = forecast.get("current");
        if (current instanceof Map<?, ?> cur) {
            Object temp = cur.get("temperature_2m");
            if (temp instanceof Number t) summary.append("Hiện tại ").append(t.intValue()).append("°C. ");
        }
        Object daily = forecast.get("daily");
        if (daily instanceof Map<?, ?> d) {
            Object maxList = d.get("temperature_2m_max");
            Object minList = d.get("temperature_2m_min");
            Object codeList = d.get("weather_code");
            if (maxList instanceof List<?> maxs && !maxs.isEmpty() && minList instanceof List<?> mins && !mins.isEmpty()) {
                Number max0 = maxs.get(0) instanceof Number n ? n : null;
                Number min0 = mins.get(0) instanceof Number n ? n : null;
                if (max0 != null && min0 != null)
                    summary.append("Dự báo 3 ngày tới: nhiệt độ ").append(min0.intValue()).append("-").append(max0.intValue()).append("°C. ");
            }
            if (codeList instanceof List<?> codes && !codes.isEmpty() && codes.get(0) instanceof Number code) {
                summary.append(weatherCodeToText(code.intValue()));
            }
        }
        if (summary.isEmpty()) summary.append("Nắng nhẹ, nhiệt độ 26-32°C.");

        List<WeatherForecastDto.DayForecast> days = List.of();
        if (daily instanceof Map<?, ?> d) {
            Object time = d.get("time");
            Object maxList = d.get("temperature_2m_max");
            Object minList = d.get("temperature_2m_min");
            Object codeList = d.get("weather_code");
            if (time instanceof List<?> times && maxList instanceof List<?> maxs && minList instanceof List<?> mins && codeList instanceof List<?> codes) {
                days = new java.util.ArrayList<>();
                String[] labels = {"Hôm nay", "Ngày mai", "Ngày kia"};
                for (int i = 0; i < Math.min(3, Math.min(times.size(), Math.min(maxs.size(), mins.size()))); i++) {
                    Number maxN = maxs.get(i) instanceof Number n ? n : null;
                    Number minN = mins.get(i) instanceof Number n ? n : null;
                    Number codeN = i < codes.size() && codes.get(i) instanceof Number n ? n : null;
                    days.add(WeatherForecastDto.DayForecast.builder()
                            .date(i < labels.length ? labels[i] : String.valueOf(times.get(i)))
                            .condition(codeN != null ? weatherCodeToText(codeN.intValue()) : "—")
                            .tempMin(minN != null ? minN.intValue() + "°C" : "—")
                            .tempMax(maxN != null ? maxN.intValue() + "°C" : "—")
                            .build());
                }
            }
        }

        return WeatherForecastDto.builder()
                .destination(destination)
                .summary(summary.toString().trim())
                .days(days)
                .build();
    }

    private static String weatherCodeToText(int wmo) {
        if (wmo == 0) return "Trời quang.";
        if (wmo <= 3) return "Có mây.";
        if (wmo <= 49) return "Sương mù.";
        if (wmo <= 59) return "Mưa phùn.";
        if (wmo <= 69) return "Mưa.";
        if (wmo <= 79) return "Mưa đá/tuyết.";
        if (wmo <= 84) return "Mưa rào.";
        if (wmo <= 94) return "Mưa/giông.";
        if (wmo <= 99) return "Giông.";
        return "Nắng, mây thay đổi.";
    }
}

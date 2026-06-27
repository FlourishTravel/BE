package com.flourishtravel.domain.chatbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gọi Overpass API (OpenStreetMap) để tìm địa điểm quanh tọa độ. Không cần API key.
 */
@Component
@Slf4j
public class OverpassClient {

    private static final String INTERPRETER_URL = "https://overpass-api.de/api/interpreter";

    private final WebClient webClient;

    public OverpassClient(@Qualifier("overpassWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

  /**
   * Tìm POI (restaurant, cafe...) trong bán kính 2km. Trả phần tử đầu tiên có name.
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> findNearbyPoi(double lat, double lon, String amenityType) {
    List<Map<String, Object>> list = findNearbyPois(lat, lon, 2000, List.of("RESTAURANT", "CAFE"), 1);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * Tìm nhiều POI trong bán kính (mét). categories: RESTAURANT, CAFE, ATTRACTION, PHOTO_SPOT, SHOPPING, RESTROOM.
   */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> findNearbyPois(
      double lat, double lon, int radiusMeters, List<String> categories, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 25));
    String around = "(around:" + radiusMeters + "," + lat + "," + lon + ")";
    String union = buildUnionQuery(around, categories);
    String query = "[out:json][timeout:15];(" + union + ");out center " + safeLimit + ";";
    try {
      String formBody = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
      Map<String, Object> body =
          webClient
              .post()
              .uri(INTERPRETER_URL)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .bodyValue(formBody)
              .retrieve()
              .bodyToMono(Map.class)
              .block();
      if (body == null
          || !(body.get("elements") instanceof List<?> elements)
          || elements.isEmpty()) {
        return List.of();
      }
      List<Map<String, Object>> out = new ArrayList<>();
      for (Object el : elements) {
        if (!(el instanceof Map<?, ?> m)) continue;
        Map<String, Object> poi = (Map<String, Object>) m;
        Map<String, Object> tags =
            poi.get("tags") instanceof Map<?, ?> t ? (Map<String, Object>) t : Map.of();
        if (tags.get("name") == null || tags.get("name").toString().isBlank()) continue;
        out.add(poi);
        if (out.size() >= safeLimit) break;
      }
      return out;
    } catch (Exception e) {
      log.warn("Overpass multi query failed lat={} lon={}: {}", lat, lon, e.getMessage());
    }
    return List.of();
  }

  static String buildUnionQuery(String around, List<String> categories) {
    Set<String> clauses = new LinkedHashSet<>();
    List<String> cats =
        categories == null || categories.isEmpty()
            ? List.of("RESTAURANT", "CAFE", "ATTRACTION")
            : categories;
    for (String raw : cats) {
      if (raw == null) continue;
      switch (raw.trim().toUpperCase()) {
        case "RESTAURANT" -> {
            clauses.add("node" + around + "[\"amenity\"~\"restaurant|fast_food\"];");
            clauses.add("way" + around + "[\"amenity\"~\"restaurant|fast_food\"];");
        }
        case "CAFE" -> {
            clauses.add("node" + around + "[\"amenity\"=\"cafe\"];");
            clauses.add("way" + around + "[\"amenity\"=\"cafe\"];");
        }
        case "ATTRACTION" -> {
            clauses.add("node" + around + "[\"tourism\"~\"attraction|museum\"];");
            clauses.add("way" + around + "[\"tourism\"~\"attraction|museum\"];");
        }
        case "PHOTO_SPOT" -> {
            clauses.add("node" + around + "[\"tourism\"=\"viewpoint\"];");
            clauses.add("way" + around + "[\"tourism\"=\"viewpoint\"];");
        }
        case "SHOPPING" -> {
            clauses.add("node" + around + "[\"shop\"];");
            clauses.add("way" + around + "[\"shop\"];");
        }
        case "RESTROOM" -> {
            clauses.add("node" + around + "[\"amenity\"=\"toilets\"];");
            clauses.add("way" + around + "[\"amenity\"=\"toilets\"];");
        }
        default -> {}
      }
    }
    if (clauses.isEmpty()) {
      clauses.add("node" + around + "[\"amenity\"~\"restaurant|cafe|fast_food\"];");
      clauses.add("way" + around + "[\"amenity\"~\"restaurant|cafe|fast_food\"];");
    }
    return String.join("", clauses);
  }
}

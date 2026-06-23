package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest;
import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import com.flourishtravel.domain.flora.repository.UserTravelPreferenceRepository;
import com.flourishtravel.domain.flora.repository.UserLocationPingRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTravelPreferenceService {

    private final UserTravelPreferenceRepository repository;
    private final UserRepository userRepository;
    private final UserLocationPingRepository locationPingRepository;

    @Transactional(readOnly = true)
    public TravelPreferencesDto getForUser(UUID userId) {
        return toDto(repository.findByUserId(userId).orElse(null));
    }

    @Transactional
    public TravelPreferencesDto update(UUID userId, UpdateTravelPreferencesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserTravelPreference pref = repository.findByUser(user).orElseGet(() ->
                UserTravelPreference.builder().user(user).build());

        if (request.getTravelStyles() != null) pref.setTravelStyles(joinList(request.getTravelStyles()));
        if (request.getBudgetLevel() != null) pref.setBudgetLevel(trimOrNull(request.getBudgetLevel()));
        if (request.getFavoriteDestinations() != null) pref.setFavoriteDestinations(joinList(request.getFavoriteDestinations()));
        if (request.getFavoriteFoods() != null) pref.setFavoriteFoods(joinList(request.getFavoriteFoods()));
        if (request.getFoodDislikes() != null) pref.setFoodDislikes(joinList(request.getFoodDislikes()));
        if (request.getFoodAllergies() != null) pref.setFoodAllergies(joinList(request.getFoodAllergies()));
        if (request.getPreferredActivities() != null) pref.setPreferredActivities(joinList(request.getPreferredActivities()));
        if (request.getAvoidedActivities() != null) pref.setAvoidedActivities(joinList(request.getAvoidedActivities()));
        if (request.getTravelPace() != null) pref.setTravelPace(trimOrNull(request.getTravelPace()));
        if (request.getTravelingWithChildren() != null) pref.setTravelingWithChildren(request.getTravelingWithChildren());
        if (request.getTravelingWithElderly() != null) pref.setTravelingWithElderly(request.getTravelingWithElderly());
        if (request.getNotificationConsent() != null) pref.setNotificationConsent(request.getNotificationConsent());
        if (request.getLocationConsent() != null) {
            pref.setLocationConsent(request.getLocationConsent());
            if (Boolean.FALSE.equals(request.getLocationConsent())) {
                locationPingRepository.deleteAllByUserId(userId);
            }
        }
        if (request.getPersonalizationConsent() != null) pref.setPersonalizationConsent(request.getPersonalizationConsent());

        pref = repository.save(pref);
        return toDto(pref);
    }

    public String buildPreferenceHint(UUID userId) {
        UserTravelPreference pref = repository.findByUserId(userId).orElse(null);
        if (pref == null) return "";
        StringBuilder sb = new StringBuilder("Sở thích du lịch (Flora):\n");
        appendLine(sb, "Phong cách", pref.getTravelStyles());
        appendLine(sb, "Ngân sách", pref.getBudgetLevel());
        appendLine(sb, "Địa điểm yêu thích", pref.getFavoriteDestinations());
        appendLine(sb, "Món thích", pref.getFavoriteFoods());
        appendLine(sb, "Món không thích", pref.getFoodDislikes());
        appendLine(sb, "Dị ứng", pref.getFoodAllergies());
        appendLine(sb, "Hoạt động ưa thích", pref.getPreferredActivities());
        appendLine(sb, "Hoạt động tránh", pref.getAvoidedActivities());
        appendLine(sb, "Nhịp đi", pref.getTravelPace());
        if (Boolean.TRUE.equals(pref.getTravelingWithChildren())) sb.append("- Đi cùng trẻ nhỏ\n");
        if (Boolean.TRUE.equals(pref.getTravelingWithElderly())) sb.append("- Đi cùng người cao tuổi\n");
        return sb.toString();
    }

    private static void appendLine(StringBuilder sb, String label, String csv) {
        if (csv != null && !csv.isBlank()) sb.append("- ").append(label).append(": ").append(csv).append("\n");
    }

    private static TravelPreferencesDto toDto(UserTravelPreference pref) {
        if (pref == null) {
            return TravelPreferencesDto.builder()
                    .notificationConsent(true)
                    .locationConsent(false)
                    .personalizationConsent(true)
                    .travelStyles(Collections.emptyList())
                    .favoriteDestinations(Collections.emptyList())
                    .favoriteFoods(Collections.emptyList())
                    .foodDislikes(Collections.emptyList())
                    .foodAllergies(Collections.emptyList())
                    .preferredActivities(Collections.emptyList())
                    .avoidedActivities(Collections.emptyList())
                    .build();
        }
        return TravelPreferencesDto.builder()
                .travelStyles(splitList(pref.getTravelStyles()))
                .budgetLevel(pref.getBudgetLevel())
                .favoriteDestinations(splitList(pref.getFavoriteDestinations()))
                .favoriteFoods(splitList(pref.getFavoriteFoods()))
                .foodDislikes(splitList(pref.getFoodDislikes()))
                .foodAllergies(splitList(pref.getFoodAllergies()))
                .preferredActivities(splitList(pref.getPreferredActivities()))
                .avoidedActivities(splitList(pref.getAvoidedActivities()))
                .travelPace(pref.getTravelPace())
                .travelingWithChildren(pref.getTravelingWithChildren())
                .travelingWithElderly(pref.getTravelingWithElderly())
                .notificationConsent(pref.getNotificationConsent())
                .locationConsent(pref.getLocationConsent())
                .personalizationConsent(pref.getPersonalizationConsent())
                .build();
    }

    static List<String> splitList(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    static String joinList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

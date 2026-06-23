package com.flourishtravel.domain.flora;

public final class FloraRecommendationConstants {

    private FloraRecommendationConstants() {}

    public static final String LOCATION_USER = "USER_LOCATION";
    public static final String LOCATION_ACTIVITY = "ACTIVITY_LOCATION";
    public static final String LOCATION_DESTINATION = "DESTINATION_FALLBACK";
    public static final String LOCATION_UNAVAILABLE = "UNAVAILABLE";

    public static final String SOURCE_OSM = "OSM";
    public static final String SOURCE_DESTINATION = "DESTINATION_DATA";
    public static final String SOURCE_CATALOG = "CATALOG";
    public static final String SOURCE_STATIC = "STATIC_FALLBACK";

    public static final String FOOD_MATCH = "MATCH";
    public static final String FOOD_UNKNOWN = "UNKNOWN";
    public static final String FOOD_EXCLUDED = "EXCLUDED";

    public static final String BUDGET_MATCH = "MATCH";
    public static final String BUDGET_UNKNOWN = "UNKNOWN";

    public static final String TRAVEL_ESTIMATE_WARNING =
            "Thời gian di chuyển chỉ là ước tính theo khoảng cách thẳng, không phải chỉ đường thực tế.";
    public static final String FOOD_ALLERGY_WARNING =
            "Thông tin món ăn và dị ứng cần được kiểm tra trực tiếp với quán trước khi gọi món.";
    public static final String SCHEDULE_UNCONFIRMED_WARNING =
            "Flora chưa có giờ và điểm tập trung đã xác nhận, nên chưa thể đảm bảo bạn có đủ thời gian ghé địa điểm này.";

    public static final String ACTION_OPEN_NEARBY = "OPEN_NEARBY_RECOMMENDATIONS";
}

package com.flourishtravel.domain.flora;

public final class FloraScheduleConstants {

    private FloraScheduleConstants() {}

    public static final String SCHEDULE_CONFIRMED = "CONFIRMED";
    public static final String SCHEDULE_ESTIMATED = "ESTIMATED";
    public static final String SCHEDULE_UNAVAILABLE = "UNAVAILABLE";

    public static final String JOURNEY_UPCOMING = "UPCOMING";
    public static final String JOURNEY_ACTIVE = "ACTIVE";
    public static final String JOURNEY_COMPLETED = "COMPLETED";
    public static final String JOURNEY_NOT_AVAILABLE = "NOT_AVAILABLE";

    public static final String EVENT_DEPARTURE = "DEPARTURE";
    public static final String EVENT_ACTIVITY = "ACTIVITY";
    public static final String EVENT_MEETING = "MEETING";
    public static final String EVENT_RETURN_TO_BUS = "RETURN_TO_BUS";
    public static final String EVENT_CHECK_IN = "CHECK_IN";
    public static final String EVENT_CHECK_OUT = "CHECK_OUT";

    public static boolean isMeetingEventType(String eventType) {
        return EVENT_DEPARTURE.equals(eventType)
                || EVENT_MEETING.equals(eventType)
                || EVENT_RETURN_TO_BUS.equals(eventType)
                || EVENT_CHECK_IN.equals(eventType)
                || EVENT_CHECK_OUT.equals(eventType);
    }
}

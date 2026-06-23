# Flora AI — Journey Accuracy (Phase 1.2)

## Overview

Flora journey now resolves **activity-level** schedule from `TourActivity` records (times, locations, gathering flags) combined with `TourSession` dates. The global `FLORA_GATHERING_HOUR` default is **no longer** used for automatic reminders or countdowns.

## Data-source precedence

1. **Session-specific schedule** — not in schema yet; skipped.
2. **Confirmed `TourActivity`** — `scheduleStatus=CONFIRMED` with `startTime`, gathering flag, location.
3. **Itinerary activity data** — times/locations with `ESTIMATED` when status unset.
4. **Booking `pickupAddress`** — **first-day DEPARTURE only**, always `ESTIMATED`, never auto-reminder.
5. **Estimated** — explicitly labeled in API/UI.
6. **Unavailable** — missing time or meeting point; clear messaging, no fake countdown.

## `scheduleStatus` meanings

| Value | UI badge | Auto 30/15/5 reminders |
|-------|----------|-------------------------|
| `CONFIRMED` | Đã xác nhận | Yes, if meeting point + time present |
| `ESTIMATED` | Dự kiến | No |
| `UNAVAILABLE` | Chưa có lịch tập trung | No |

## `journeyStatus`

| Value | Meaning |
|-------|---------|
| `UPCOMING` | Before session start |
| `ACTIVE` | Paid/confirmed and within session dates |
| `COMPLETED` | After session end |
| `NOT_AVAILABLE` | Missing session or ineligible booking |

## Automatic reminders

Sent only when `nextMeeting.reminderEligible = true`:

- `scheduleStatus = CONFIRMED`
- Confirmed meeting time (`startTime` on gathering activity)
- Confirmed meeting point (`locationName` non-empty)

Messages include meeting point name only when confirmed. Idempotency key includes meeting `Instant` epoch — schedule changes allow new reminders without duplicates.

**Not sent:**

- `ESTIMATED` or `UNAVAILABLE` meetings
- Legacy tours without gathering activities
- Pickup-only departure fallback
- Former 07:00 default gathering

## Legacy tours (incomplete data)

- Day-level `currentScheduleItem` / `nextScheduleItem` preserved.
- `currentActivity` / `nextActivity` null if no activity times.
- `nextMeeting` null or `ESTIMATED` pickup on day 1 only.
- `warnings` explain missing detail.
- No automatic reminders until admin marks gathering activities `CONFIRMED`.

## Admin data entry

In **Tour Itinerary Builder** (`PUT /api/tours/admin/{id}/itinerary`), per activity:

- Start / end time
- Location name + optional address
- Optional lat/lon (not required)
- **Hoạt động tập trung / lên xe** checkbox
- Gathering type (DEPARTURE, RETURN_TO_BUS, …)
- Schedule status (CONFIRMED / ESTIMATED / UNAVAILABLE)

Hibernate `ddl-auto: update` adds columns: `is_gathering_event`, `gathering_event_type`, `location_address`, `schedule_status`.

## Configuration

```yaml
app:
  flora:
    journey:
      safety-buffer-minutes: 10  # FLORA_JOURNEY_SAFETY_BUFFER_MINUTES
```

Used for `freeMinutesUntilMeeting` during free exploration activities.

## API compatibility

`GET /api/flora/bookings/{bookingId}/journey` — all existing fields retained. New optional fields:

- `journeyStatus`, `currentActivity`, `nextActivity`, `warnings`, `freeMinutesUntilMeeting`
- Extended `nextMeeting` (`eventType`, `locationName`, `scheduleStatus`, `reminderEligible`, …)

## Known limitations (before Phase 2)

- No per-session schedule overrides (tour template only).
- No turn-by-turn navigation or background GPS.
- Return-to-bus uses geocoding when coordinates missing.
- Mobile mock itinerary timeline still separate from Flora API in `ScheduleScreen` (Flora panel component provided).
- Countdown on web refreshes every 60s (not second-precision).

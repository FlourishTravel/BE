# Flora AI — Journey Data Audit (pre Phase 1.2)

Audit date: 2026-06-23. Based on inspection of entities, services, admin APIs, and frontends — not entity names alone.

## Activity start / end time

| Source | Fields | Used by Flora before 1.2? |
|--------|--------|---------------------------|
| `TourActivity` | `startTime`, `endTime` (`LocalTime`) | **No** — stored and editable in admin itinerary, ignored by `FloraJourneyService` |
| `TourActivity` | `durationMinutes` | **No** |
| `TourItinerary` | `dayNumber` + session `startDate` | **Partially** — only to pick day-level `currentScheduleItem` / `nextScheduleItem` |
| `TourSession` | `startDate`, `endDate` | **Yes** — active-trip window |
| Config | `app.flora.default-gathering-hour` (07:00) | **Yes** — synthetic daily gathering for every trip day |

**Gap:** No absolute `Instant` per activity; Flora must combine `session.startDate + (dayNumber-1) + LocalTime` in `Asia/Ho_Chi_Minh`.

## Activity location

| Source | Fields | Used by Flora before 1.2? |
|--------|--------|---------------------------|
| `TourActivity` | `locationName`, `latitude`, `longitude` | **No** |
| `TourLocation` | `locationName`, lat/lon, `dayNumber` | **No** |
| `Booking` | `pickupAddress` | **Yes** — treated as universal meeting point |

**Gap:** Return-to-bus during stops incorrectly used departure pickup address.

## Gathering / return-to-bus flag

| Source | Exists? |
|--------|---------|
| `TourActivity.isGatheringEvent` | **No** (added in Phase 1.2) |
| `TourActivity.gatheringEventType` | **No** (added in Phase 1.2) |
| Guide check-in `"gathering"` type | Separate from itinerary |

**Gap:** Cannot distinguish “tham quan Chợ Đà Lạt” from “tập trung lên xe” in data.

## Session meeting point & session-specific schedule

| Source | Exists? |
|--------|---------|
| Session-specific meeting point | **No** — no field on `TourSession` |
| Session-specific schedule override | **No** — no `TourSessionScheduleEvent` entity |
| Per-session itinerary | **No** — itinerary is tour-level only |

Flora uses the **tour template itinerary** for all sessions of that tour.

## `pickupAddress` suitability

| Use case | Suitable? |
|----------|-----------|
| Initial departure pickup (day 1 morning) | **Yes** — booking-level departure instruction |
| Return-to-bus at every stop | **No** — address is fixed at booking time, not per activity |
| Confirmed gathering point mid-tour | **No** — unless coincidentally the same place |

Phase 1.2 uses `pickupAddress` only for **first-day DEPARTURE** when no confirmed departure gathering activity exists, marked **ESTIMATED**.

## Missing fields for accurate gathering reminders (before 1.2)

1. Gathering flag on activities
2. Schedule confirmation status (`CONFIRMED` vs `ESTIMATED`)
3. Activity-level meeting time (not global 07:00)
4. Activity-level meeting location text + optional coordinates
5. `reminderEligible` logic tied to confirmed data
6. Invalidation when schedule changes

## Confirmed vs estimated vs unavailable (before 1.2)

| Data | Treatment before 1.2 |
|------|----------------------|
| Activity times in DB | Ignored — not exposed |
| Global 07:00 gathering | Presented as if real (`nextGatheringAt`, countdown) |
| `pickupAddress` blank | Placeholder string claiming voucher/email |
| Missing itinerary | Day items null; gathering still computed at 07:00 |

**Could not distinguish** confirmed schedule from defaults. Phase 1.2 adds explicit `scheduleStatus` and stops automatic reminders without `CONFIRMED` meeting time + point.

## Admin itinerary API (existing)

- `GET/PUT /api/tours/admin/{id}/itinerary`
- `ActivityRequest` / `ActivityRef` already support times, location, type, coordinates
- Phase 1.2 extends with `isGatheringEvent`, `gatheringEventType`, `locationAddress`, `scheduleStatus`

## Frontend state (before 1.2)

| Surface | Journey API | Activity-level UI |
|---------|-------------|-------------------|
| Web `FloraCompanion.jsx` | Yes (booking detail) | No — day titles only |
| Mobile `ScheduleScreen.kt` | No | Mock data only |

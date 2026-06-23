# Session schedule override audit (Flora AI Phase 1.5)

Audit date: 2026-06-23. Source: `Server/BE`, `Client/FE/website`, `Mobile/mobile_app`.

## 1. Guide assignment to TourSession

- `TourSession.tourGuide` → `User` (`tour_guide_id` FK).
- Assigned via `PUT /api/tour-operations/sessions/{id}/guide` (`TourOperationService.assignGuide`, **ADMIN** only).
- `GuideService.assertGuideOwnsSession` verifies `session.tourGuide.id == guideId` for all `/guide/sessions/*` mutations.

## 2. Roles that can edit tour/session data

| Role | Tour template itinerary | Session guide assign | Session schedule override (before 1.5) |
|------|-------------------------|----------------------|----------------------------------------|
| ADMIN | `PUT /api/tours/admin/{id}/itinerary` | `TourOperationController` | **None** |
| TOUR_GUIDE | No | No | **None** — read-only itinerary in `GuideSessionDetailDto` |
| TRAVELER | No | No | No |

`SecurityConfig`: `/admin/**` → ADMIN; `/guide/**` → ADMIN or TOUR_GUIDE.

## 3. Session custom schedule fields

**No** session-specific schedule entity exists before Phase 1.5. `TourSession` has only dates, capacity, status, `tourGuide` — no meeting point or per-session itinerary.

## 4. Session custom meeting point

**No** dedicated field. Flora uses `TourActivity` gathering events or booking `pickupAddress` (day-1 departure estimate only).

## 5. Activity timing storage

**Template-level only**: `TourActivity.startTime` / `endTime` (`LocalTime`) on `TourItinerary` → `Tour`. Combined with `TourSession.startDate` + day number in `FloraJourneyScheduleResolver`.

## 6. Reusable session-specific entity

**None found.** Phase 1.5 adds `TourSessionActivityOverride` (one row per session + template activity).

## 7. Traceability of published changes

Before 1.5: no publish workflow. Phase 1.5 adds `publishedAt`, `publishedByUserId`, `version` on override rows. `BaseEntity` provides `createdAt` / `updatedAt`.

## 8. Reminder delivery invalidation

- `FloraReminderDelivery.idempotencyKey` = `bookingId:reminderType:meetingEpochSecond`.
- Meeting time change → new epoch → new keys for 30/15/5 reminders (no duplicate for same instant).
- `SCHEDULE_CHANGED` type exists in `FloraReminderTypes` but **was not used** before 1.5.
- No explicit “invalidate pending” flag — reliance on journey recalculation at job time.

## 9. Traveler journey data affected by session publish

From `FloraJourneyDto` / resolver snapshot:

- `currentActivity`, `nextActivity`, `nextMeeting`
- `nextGatheringAt`, `minutesUntilGathering`, `meetingPoint`
- `freeMinutesUntilMeeting`, `warnings`
- Nearby recommendations use same journey context (`freeMinutesUntilMeeting`, `nextMeeting.scheduleStatus`)

## 10. UI extension points (no duplicate screens)

| Surface | Extend |
|---------|--------|
| Web `GuideTourDetail.jsx` | Session itinerary tab — add draft/publish per activity |
| Web admin `TourOperationController` UI | Optional admin schedule via same guide API (ADMIN on `/guide/**`) |
| Web `FloraCompanion.jsx` | Schedule-updated badge on traveler journey |
| Mobile `FloraJourneyPanel.kt` | Source badge + last updated |
| Mobile guide | No full editor in this phase — web guide surface documented |

`GuideOperations.jsx` is mock data only — **not** extended; use `GuideTourDetail.jsx`.

## Summary gap (pre-1.5)

Flora journey reads **tour template only** for all sessions (`journey-data-audit.md`, `journey-accuracy.md`). Phase 1.5 adds published per-session overrides without mutating `TourActivity`.

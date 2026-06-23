# Flora AI Phase 1.5 — Session schedule overrides

Schema is managed through Hibernate `ddl-auto: update` (no Flyway migration in this repo).

## Data model

**Entity:** `TourSessionActivityOverride` (`tour_session_activity_overrides`)

- One row per `(tourSessionId, tourActivityId)` — unique constraint.
- References existing template `TourActivity`; does **not** copy the full itinerary.
- Override fields are nullable; unset fields fall back to template at resolution time.
- `publicationStatus`: `DRAFT` | `PUBLISHED` | `CANCELLED`
- `scheduleStatus` on override: `CONFIRMED` | `ESTIMATED` | `UNAVAILABLE`
- `version`, `publishedAt`, `publishedBy` for audit and notification idempotency.

## Template vs session override

| Surface | Template (`TourActivity`) | Draft override | Published override |
|--------|---------------------------|----------------|--------------------|
| Guide/admin schedule API | Shown as “Lịch mẫu” | Shown as “Bản nháp” | Shown as “Đã công bố” |
| Flora journey (traveler) | Used when no override | **Ignored** | **Merged** |
| Flora reminders | From resolved journey | **Ignored** | From resolved journey |
| In-app `SCHEDULE_CHANGED` | — | **No** | **Yes** (if material) |
| Nearby safety | From resolved journey | **Ignored** | From resolved journey |

Publishing never updates `TourActivity` rows.

## Schedule precedence (`FloraJourneyScheduleResolver`)

1. Published session override with `CONFIRMED` data  
2. Published session override with `ESTIMATED` data  
3. Confirmed template `TourActivity`  
4. Estimated template `TourActivity`  
5. Day-one booking pickup (departure only, `ESTIMATED`, `BOOKING_PICKUP_ESTIMATE`)  
6. `UNAVAILABLE`

Cancelled published overrides are excluded from current/next activity selection.

Optional journey fields: `scheduleSource`, `scheduleVersion`, `lastUpdatedAt`, `lastUpdatedReason`.

`scheduleSource` values: `SESSION_OVERRIDE`, `TOUR_TEMPLATE`, `BOOKING_PICKUP_ESTIMATE`, `UNAVAILABLE`.

## Authorization

| Role | Schedule APIs |
|------|----------------|
| `ADMIN` | Any session — view, draft, publish, cancel |
| `TOUR_GUIDE` | Assigned sessions only (`GuideService` ownership) |
| `TRAVELER` | No schedule edit; journey API shows published data only |

JWT principal is used; never trust user IDs from request bodies.

## APIs

Under existing `GuideController` (`/api/guide/...`):

- `GET /sessions/{sessionId}/schedule`
- `PATCH /sessions/{sessionId}/schedule/activities/{activityId}` — save draft
- `POST /sessions/{sessionId}/schedule/activities/{activityId}/publish`
- `POST /sessions/{sessionId}/schedule/activities/{activityId}/cancel`

Admins use the same endpoints (role check in `TourSessionScheduleService`).

## Reminders and notifications

- Meeting reminders use idempotency `bookingId:type:meetingEpochSecond` — a new meeting instant yields new keys.
- On material publish, pending deliveries for the **previous** meeting instant are deleted when possible.
- `SCHEDULE_CHANGED` idempotency: `bookingId:SCHEDULE_CHANGED:sessionId:activityId:version`
- Sent only when: active booking, `notificationConsent`, material change, version not previously notified.
- Re-publishing unchanged effective schedule does not bump version or re-notify.

## Traveler privacy

- Journey API does not expose draft overrides, operational notes, or publisher audit to travelers (`lastUpdatedReason` uses published operational note only when exposed via journey metadata).
- No cross-traveler booking data.

## UI surfaces

- **Web guide:** `GuideTourDetail.jsx` — session schedule edit (draft / publish / cancel).
- **Web traveler:** `FloraCompanion.jsx` — “Lịch trình đã cập nhật” when `scheduleSource === SESSION_OVERRIDE`.
- **Android traveler:** `FloraJourneyPanel.kt` — schedule badge, last updated, refresh.
- **Mobile guide editor:** not in scope; web guide is the operational editing surface.

## Known limitations

- No ad-hoc session-only activities without a template `TourActivity` (single override model only).
- No push/FCM/SMS/email for schedule changes — in-app only.
- Reminder invalidation is keyed by previous meeting **instant**; same-time location-only changes rely on `SCHEDULE_CHANGED`.
- Hibernate `ddl-auto: update` — production should plan explicit migrations when stabilised.

## Legacy behavior

- Tours without activity times still return journey with warnings; no invented default gathering hour.
- Existing journey JSON fields remain; new metadata fields are optional for clients.

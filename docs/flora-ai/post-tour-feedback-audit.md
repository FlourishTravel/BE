# Post-tour feedback audit (Flora AI Phase 1.4)

Audit date: 2026-06-23. Source: `Server/BE`, `Client/FE/website`, `Mobile/mobile_app`.

## 1. Booking / tour completion

| Signal | Location | Rule |
|--------|----------|------|
| Booking status | `Booking.status` | Flow: `pending → paid → confirmed → completed` (also `cancelled`) |
| Admin transition | `AdminBookingService` | `confirmed → completed` is a valid admin transition |
| Session dates | `TourSession.startDate` / `endDate` | Journey active while `today` ∈ [start, end] (`FloraJourneyService.isActiveTrip`) |
| Post-tour reminder query | `BookingRepository.findRecentlyCompletedForFlora` | `status = completed` AND `session.endDate = yesterday` |
| Review eligibility (before 1.4) | `ReviewService.create` | Allowed when `paid` OR `completed` (no session-end check) |

**Phase 1.4 rule:** A booking is eligible for post-tour feedback when it is not cancelled, has no invalidating refund, and either `status = completed` or (`paid`/`confirmed` with `session.endDate` before today in `app.flora.timezone`). Date-passed alone is not enough without a valid paid/confirmed/completed status.

## 2. Duplicate reviews

- `Review.booking` is `@OneToOne` with unique index on `booking_id`.
- `ReviewRepository.existsByBooking(Booking)` blocks duplicates.
- `ReviewService` throws `BadRequestException("Bạn đã đánh giá đơn này rồi")`.
- **One review per booking** — no edit endpoint exists; submitted feedback is read-only.

## 3. `POST /api/reviews` linkage

- Request: `bookingId`, `rating`, `comment`.
- Service sets `tour` from `booking.session.tour`.
- Linked to **booking** (primary); tour is derived, not supplied by client.

## 4. Review fields and rules

| Field | Type | Notes |
|-------|------|-------|
| `rating` | 1–5 | Validated in service + `@Min/@Max` on DTO |
| `comment` | TEXT, optional | Trimmed; no media, moderation, or public listing in user flow |
| `booking`, `user`, `tour` | relations | Required |
| `feedbackTags` | **Added in 1.4** | Comma-separated known tag IDs |

No separate admin review reporting module found for user-submitted tour reviews.

## 5. Booking ownership

- `ReviewService` checks `booking.getUser().getId().equals(userId)`.
- Flora endpoints use `FloraPrivacyService.requireOwnedBooking`.

## 6. Notification system for post-tour reminder

- `FloraReminderService.checkPostTourFeedback()` — scheduled job, in-app only.
- `NotificationService.createFloraNotification` — `data` JSON includes `bookingId`.
- Idempotency: `FloraReminderDelivery.idempotencyKey` = `bookingId:POST_TOUR_FEEDBACK:0`.
- **Gaps fixed in 1.4:** skip when review exists; spec title/body; respect `notificationConsent` (already checked).

## 7. Safe completion recognition

- Reminder job: only `completed` + session ended yesterday.
- Feedback UI/API: completed status OR ended session with paid/confirmed — not inferred from departure alone on `pending`/`cancelled`.

## 8. Preference fields safe to suggest (explicit chips only)

| Chip theme | Field | Example value |
|------------|-------|---------------|
| Photo spots | `preferredActivities` | `chụp ảnh` |
| Coffee | `preferredActivities` | `cà phê` |
| Local cuisine | `preferredActivities` | `ẩm thực địa phương` |
| Resort | `travelStyles` | `Nghỉ dưỡng` |
| Nature | `preferredActivities` | `thiên nhiên` |
| Relaxed pace | `travelPace` | `chậm` |
| Active pace | `travelPace` | `nhanh` |
| Dislike much travel | `avoidedActivities` | `di chuyển nhiều` |
| Dislike rushed | `avoidedActivities` | `lịch trình gấp` |
| Want free time | `preferredActivities` | `thời gian tự do` |

Uses existing conventions: `travelPace` ∈ {`chậm`, `vừa phải`, `nhanh`}; `travelStyles` from mobile settings list.

## 9. Fields never inferred automatically

`foodAllergies`, `foodDislikes`, `favoriteFoods`, `budgetLevel`, `travelingWithChildren`, `travelingWithElderly`, `notificationConsent`, `locationConsent`, `personalizationConsent`, health/religion/ethnicity/politics/family/finance — **never** set from ratings, messages, or chips without explicit user PATCH.

## 10. Reuse vs extensions

| Reuse | Extension |
|-------|-----------|
| `POST /api/reviews` + `Review` entity | Optional `feedbackTags` column |
| `GET/PATCH /api/flora/preferences/me` | `POST /api/flora/feedback/preference-preview` |
| `FloraReminderService` + delivery table | Skip-if-reviewed, title/body copy |
| `FloraPrivacyService` ownership | — |
| **New** `GET /api/flora/bookings/{bookingId}/post-tour-feedback` | Eligibility + tag catalog + existing review summary |

No duplicate `FloraPostTourFeedback` entity — feedback lives on `Review`.

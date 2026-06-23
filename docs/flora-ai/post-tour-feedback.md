# Post-tour feedback (Flora AI Phase 1.4)

## Overview

After a completed trip, Flora invites the user to rate the tour and optionally share feedback chips. Preference learning is **consent-based** and **explicit** — chips never update the database until the user taps **Lưu sở thích này** and `PATCH /api/flora/preferences/me` runs.

## Eligibility

All must be true:

- Authenticated user owns the booking
- Booking not `cancelled`
- No approved/completed refund on the booking
- Status is `paid`, `confirmed`, or `completed` **and** `session.endDate` is before today (`app.flora.timezone`)

Upcoming, cancelled, or refunded bookings are not eligible.

## Data model

- **Tour feedback** → `Review` entity (`POST /api/reviews`)
  - `rating` (1–5), `comment`, optional `feedbackTags` (comma-separated catalog IDs)
  - One review per booking (`booking_id` unique)
- **Preference suggestions** → deterministic mapping from selected tags (`FloraFeedbackTagCatalog`)
- **Confirmed updates** → `PATCH /api/flora/preferences/me` only

No separate `FloraPostTourFeedback` table.

## APIs

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/flora/bookings/{bookingId}/post-tour-feedback` | Eligibility, tags, existing review |
| POST | `/api/reviews` | Submit rating + comment + optional tags |
| POST | `/api/flora/feedback/preference-preview` | Preview merged preferences from selected tags |
| PATCH | `/api/flora/preferences/me` | Save preferences after explicit confirmation |

## User flow

1. User opens booking detail (web) or trip detail (mobile) for an eligible booking.
2. Flora shows intro: *Chuyến đi của bạn đã kết thúc rồi…*
3. User rates, adds optional comment, selects chips.
4. **Đánh giá chuyến đi** → `POST /api/reviews` (with or without tags).
5. If `personalizationConsent = true` and chips selected → preview suggested changes.
6. **Lưu sở thích này** → `PATCH /api/flora/preferences/me` with merged partial body.
7. **Chỉ gửi đánh giá** → step 4 only.

If `personalizationConsent = false`: review allowed; no chips or preference preview shown.

If review already exists: show summary or message *Bạn đã gửi đánh giá cho chuyến đi này rồi…*

## Reminder idempotency

- In-app notification only (`POST_TOUR_FEEDBACK` type)
- Sent when: booking `completed`, session ended yesterday, `notificationConsent = true`, no review, no prior delivery for `bookingId:POST_TOUR_FEEDBACK:0`
- Title: *Flora muốn nghe cảm nhận của bạn*
- `data.bookingId` for deep link

## Privacy boundaries

- No inference of allergies, health, religion, ethnicity, politics, family, or finance from feedback
- No food/allergy safety claims from ratings
- No LLM or behavioral auto-updates to preferences
- Review comments not logged on mobile

## Known limitations

- No review edit after submit (existing policy)
- Reminder job only considers `completed` status + yesterday's `endDate` (stricter than feedback UI which also allows ended `paid`/`confirmed`)
- No push/FCM/email in this phase
- Hibernate `ddl-auto: update` adds `reviews.feedback_tags` column automatically

See also: [post-tour-feedback-audit.md](./post-tour-feedback-audit.md)

# Flora AI — Privacy & Consent

## Mandatory rules

- Flora never references microphone or “listening”.
- Only data stored in Flourish-Travel and authorized for the authenticated user.
- Booking-specific endpoints verify `booking.user.id == principal.id`.
- GPS processed only when `locationConsent == true`.
- LLM prompts built server-side; no API keys on frontend.
- Other travelers’ PII, payment, phone, or GPS never included in Flora context.

## Consent flags (`user_travel_preferences`)

| Field | Purpose |
|-------|---------|
| `notificationConsent` | Flora proactive reminders (UC03, UC05, UC07) |
| `locationConsent` | GPS ping + nearby/return-to-bus (UC04, UC05) |
| `personalizationConsent` | Booking/favorite/preference data in LLM context |

Default: all `true` for new rows (user can opt out via PATCH).

## Location retention

- Only latest ping per `(userId, bookingId)` kept active during trip.
- Pings older than `app.flora.location-retention-hours` (default 48h) deleted by scheduled cleanup.
- Revoking `locationConsent` deletes pings for that user.

## LLM data minimization

`FloraPrivacyService` strips email, payment fields, emergency contacts, and other guest data before prompt assembly.

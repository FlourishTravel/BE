# Flora AI MVP Overview

**Flourish-Travel** — travel booking platform  
**Flora AI** — personalized travel companion (before, during, after tour)

Tagline: *Flora AI – Người bạn đồng hành thông minh cho mọi chuyến đi.*

## Architecture

```
Client (FE / Mobile)
  → POST /api/chatbot/message (+ optional bookingId, GPS)
  → GET  /api/flora/bookings/{id}/journey
  → POST /api/flora/bookings/{id}/location
  → PATCH /api/users/me/travel-preferences

FloraController / UserController
  → FloraJourneyService / FloraLocationService / UserTravelPreferenceService
  → FloraPrivacyService (ownership + consent)
  → FloraContextBuilder → ChatbotService → LlmService (OpenRouter)
  → FloraReminderService (@Scheduled) → NotificationService
```

## Use cases (MVP scope)

| ID | Feature | Status |
|----|---------|--------|
| UC01 | Personalized tour recommendation | LLM + catalog/destination/planner reuse |
| UC02 | Active booking journey briefing | `GET /flora/bookings/{id}/journey` |
| UC03 | Gathering reminders | Scheduled job + idempotent notifications |
| UC04 | Nearby places (GPS) | `nearby-places` + location ping |
| UC05 | Return-to-bus alert | Location vs meeting point + threshold |
| UC06 | Flora chat | Extended `/chatbot/message` |
| UC07 | Post-tour feedback | Prompt + notification type `POST_TOUR_FEEDBACK` |

## Known limitations

- No real-time map routing (straight-line distance only).
- Restaurant data from OSM Overpass; may be sparse without `GOOGLE_MAPS_API_KEY`.
- Gathering time defaults to 07:00 Asia/Ho_Chi_Minh when not in DB.
- Requires `OPENROUTER_API_KEY` for full LLM replies.

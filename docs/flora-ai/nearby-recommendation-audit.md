# Flora AI — Nearby Recommendation Audit (pre Phase 1.3)

Audit date: 2026-06-23. Based on code inspection.

## 1. Nearby POI sources today

| Source | Provider | Used by |
|--------|----------|---------|
| Overpass/OSM | `OverpassClient` | `ChatbotDataService.getNearbyPlace()` |
| Open-Meteo geocode | `OpenMeteoClient.geocode()` | Resolve destination name → lat/lon |
| Static mock fallback | Hard-coded in `ChatbotDataService` | When OSM/geocode fails |
| Destination DB POIs | `Destination.mapPois` | `DestinationService` (not wired to chatbot nearby) |
| Catalog | `CatalogService.floraRecommend()` | Tour recommendations, not lat/lon POIs |

**GET `/api/chatbot/nearby-places`** returns a single `NearbyPlaceDto` with approximate distance text, optional mock data.

## 2. Data reliability

| Type | Real-time? | Labeled? |
|------|------------|----------|
| OSM Overpass | Yes (community) | No label in API |
| Static fallback | No | No — presented like real |
| Destination map POIs | Curated DB | Not exposed to nearby flow |
| Mock mobile itinerary | No | Local mock only |

## 3. POI fields available

**OSM (Overpass):** `name`, `amenity`, `tourism`, `addr:street`, lat/lon, element id. No ratings, prices, opening hours, or allergen data in current client.

**NearbyPlaceDto:** `name`, `type`, `distance` (text), `rating` (hard-coded 4.0), `address`.

**DestinationMapPoi:** `category`, `name`, `rating`, `priceLabel`, lat/lon — usable as secondary source.

## 4. Preference fields (`UserTravelPreference`)

`travelStyles`, `budgetLevel`, `favoriteFoods`, `foodDislikes`, `foodAllergies`, `preferredActivities`, `avoidedActivities`, `travelPace`, `travelingWithChildren`, `travelingWithElderly`, `personalizationConsent`, `locationConsent`.

## 5. User location

- **POST `/api/flora/bookings/{id}/location`** stores one ping per user/booking (replaced on update), requires `locationConsent`.
- Chatbot may receive lat/lon in body but does not persist via message endpoint.
- No background tracking.

## 6. Activity / meeting coordinates

- `TourActivity.latitude/longitude` — available when admin enters them.
- `FloraActivityDto` / `currentActivity` exposes coords in journey API.
- Meeting point often text-only; coords when activity/gathering has them.

## 7. Distance type

Straight-line (haversine) in `FloraLocationService`. No route engine. Chatbot nearby uses text "khoảng 500m".

## 8. Insufficient data guarantees

- **Allergy safety:** OSM has no verified allergen menus — cannot guarantee.
- **Restaurant availability / hours:** Not in current Overpass query.
- **Exact walk time:** No Directions API — only distance-based estimates.

## 9. Reusable services

| Service | Reuse for 1.3 |
|---------|----------------|
| `FloraJourneyService` | Schedule context, `freeMinutesUntilMeeting` |
| `FloraPrivacyService` | Booking ownership, consents |
| `OverpassClient` | Extend for multi-POI radius query |
| `OpenMeteoClient` | Geocode destination fallback, weather warnings |
| `ChatbotDataService` | Weather summary pattern |
| `DestinationService` | Optional destination POI fallback |
| `ChatbotRateLimitStore` | Same in-memory store, new key prefix |
| `FloraLocationService.haversineMeters` | Distance calculation |

## 10. Minimal new API

**Required:** `POST /api/flora/bookings/{bookingId}/nearby-recommendations` (authenticated, body coords — not GET query).

No new external provider in this phase.

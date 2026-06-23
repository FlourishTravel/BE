# Flora AI — Nearby recommendations (Phase 1.3)

Safe nearby POI suggestions tied to confirmed schedule windows and real journey free time.

## Data sources (priority)

1. **OSM / Overpass** — live POI query via `OverpassClient.findNearbyPois`
2. **Destination map POIs** — reserved for future enrichment (`DESTINATION_DATA`)
3. **Catalog local experiences** — reserved (`CATALOG`)
4. **Static fallback** — only when explicitly wired; labeled `STATIC_FALLBACK`

Current implementation uses **OSM only** for the recommendation endpoint. Legacy `GET /api/chatbot/nearby-places` may still return static mock fallback.

## API

`POST /api/flora/bookings/{bookingId}/nearby-recommendations` (authenticated)

Coordinates are sent in the **POST body** (not query string) to reduce accidental logging.

### Location fallback order

1. `USER_LOCATION` — request body GPS **and** `locationConsent = true`
2. `ACTIVITY_LOCATION` — `currentActivity.latitude/longitude` from journey
3. `DESTINATION_FALLBACK` — geocode `tour.destinationCity` (Open-Meteo geocoder)
4. `UNAVAILABLE` — empty `recommendations`, no invented POIs

Request GPS is **not persisted**. Raw coordinates are not logged at high precision and are not sent to the LLM.

## Schedule safety

`canValidateSchedule = true` only when:

- `nextMeeting` exists
- `scheduleStatus = CONFIRMED`
- confirmed time and location name
- `freeMinutesUntilMeeting` is available (already includes journey safety buffer from Phase 1.2)

```
requiredMinutes = estimatedRoundTripMinutes + estimatedVisitMinutes
fitsSchedule = canValidateSchedule && requiredMinutes <= freeMinutesUntilMeeting
```

When meeting is missing, `ESTIMATED`, `UNAVAILABLE`, or lacks time/location:

- `canValidateSchedule = false`
- every item `fitsSchedule = false`
- response warning: Flora chưa có giờ và điểm tập trung đã xác nhận...

**No second safety buffer** is applied in the recommendation layer.

## Travel time

Straight-line distance only (`straightLineDistanceMeters`). Round-trip minutes use configured walking speed (default 60 m/min). Every item includes:

> Thời gian di chuyển chỉ là ước tính theo khoảng cách thẳng, không phải chỉ đường thực tế.

## Food & budget

- Personalization only when `personalizationConsent = true`
- Exclude food POI only when name metadata clearly matches `foodDislikes` / `foodAllergies`
- Unknown menu → `foodMatchStatus = UNKNOWN` + allergy warning (no safety claim)
- No price data → `budgetMatchStatus = UNKNOWN`

## Weather

Optional warnings from existing `ChatbotDataService.getWeatherForecast` when destination is known. Failures are ignored (no invented forecast).

## Rate limiting

Key: `flora-nearby:user:{userId}`  
Default: 10 requests/minute (`app.flora.recommendations.rate-limit.requests-per-minute`)  
HTTP **429** with standard `ApiResponse` wrapper.

## Chatbot integration

Phrases like “Gợi ý quán ăn gần đây” with `bookingId` return action:

```json
{ "type": "OPEN_NEARBY_RECOMMENDATIONS", "label": "Xem gợi ý gần đây", "payload": "<bookingId>" }
```

The chatbot does **not** call Overpass on every message; the client calls the Flora endpoint after the user taps the action.

## Configuration

```yaml
app:
  flora:
    recommendations:
      default-radius-meters: 1000
      max-radius-meters: 3000
      default-limit: 5
      max-limit: 10
      estimated-walking-speed-meters-per-minute: 60
      rate-limit:
        requests-per-minute: 10
      default-visit-minutes:
        restaurant: 35
        cafe: 30
        attraction: 45
        photo-spot: 20
        shopping: 30
        restroom: 10
```

## Known limitations

- No turn-by-turn navigation or Google Directions
- No verified allergen/menu database
- No real-time restaurant availability or pricing
- Destination geocode is approximate
- Legacy tours without activity coordinates rely on destination fallback or return empty results
- Mobile GPS capture uses server fallback when coordinates are not yet wired to device location APIs

See also: [nearby-recommendation-audit.md](./nearby-recommendation-audit.md)

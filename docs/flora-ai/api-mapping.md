# Flora AI — API Mapping

Base path: `/api` (Spring `server.servlet.context-path`).

## Reused existing APIs

| Use case | API | Service |
|----------|-----|---------|
| UC01 Tour recommendation | `GET /tours`, `GET /tours/{id}/similar` | `TourService` |
| UC01 Destination match | `POST /destinations/flora-match` | `DestinationService.floraMatch` |
| UC01 Catalog recommend | `POST /catalog/flora-recommend` | `CatalogService.floraRecommend` |
| UC01 Planner | `POST /planner/generate`, `POST /planner/calculate-budget` | `PlannerService` |
| UC02 Booking data | `GET /bookings/me`, `GET /bookings/{id}` | `BookingService` |
| UC02 Group chat context | `GET /chat/bookings/{bookingId}/context` | `ChatService` (chat eligibility only) |
| UC04 Nearby POI | `GET /chatbot/nearby-places` | `ChatbotDataService` |
| UC04 Weather | `GET /chatbot/weather-forecast` | `ChatbotDataService` |
| UC06 Chat | `POST /chatbot/message` | `ChatbotService` + `FloraContextBuilder` |
| UC07 Reviews | `POST /reviews` | existing review flow |
| UC07 Favorites | `GET/POST/DELETE /favorites` | `FavoriteService` |
| Notifications (read) | `GET/PATCH /notifications` | `NotificationService` |
| Profile | `GET/PATCH /users/me` | `UserService` |

## New minimal APIs (MVP)

| Endpoint | Reason |
|----------|--------|
| `GET /users/me/travel-preferences` | No persisted travel prefs existed on `User` |
| `PATCH /users/me/travel-preferences` | Flora personalization + consent flags |
| `GET /flora/bookings/{bookingId}/journey` | Active-tour briefing (itinerary, countdown, weather) beyond chat context DTO |
| `POST /flora/bookings/{bookingId}/location` | Consent-based GPS ping for UC04/UC05 |

## Extended (backward compatible)

| Endpoint | New optional fields |
|----------|---------------------|
| `POST /chatbot/message` | `message`, `bookingId`, `latitude`, `longitude`, `locale`, `source` |
| Response `ChatbotResponse` | `suggestedActions`, `recommendations`, `warnings`, `nextMeeting` |

Legacy clients sending only `{ "content", "sessionId", "state" }` continue to work.

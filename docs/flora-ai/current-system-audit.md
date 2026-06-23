# Flora AI — Kiểm toán hệ thống hiện tại

> **Phạm vi:** Đọc-only, dựa trên mã nguồn thực tế trong repo `Flourish-Travel` tại thời điểm audit.  
> **Lưu ý:** Codebase đã có **một phần Flora AI MVP** (package `com.flourishtravel.domain.flora.*`, mở rộng chatbot, FE/mobile tích hợp một phần). Báo cáo mô tả **trạng thái hiện tại**, không giả định từ tên endpoint.

---

# 1. Kiến trúc dự án (Project Architecture)

| Hạng mục | Giá trị thực tế | Đường dẫn / nguồn |
|----------|----------------|-------------------|
| **Backend framework & version** | Spring Boot **3.2.5** | `Server/BE/pom.xml` (parent `spring-boot-starter-parent`) |
| **Java version** | **17** | `Server/BE/pom.xml` → `<java.version>17</java.version>` |
| **Database** | **PostgreSQL** (production); **H2** (dev profile) | `Server/BE/src/main/resources/application.yml` (datasource PostgreSQL); `application-dev.yml` (H2) |
| **ORM** | **Spring Data JPA / Hibernate** | `spring-boot-starter-data-jpa`; `spring.jpa.hibernate.ddl-auto: update` trong `application.yml` |
| **Migration tool** | **Không có Flyway/Liquibase** — schema do Hibernate `ddl-auto: update` | `Server/BE/src/main/resources/application.yml` |
| **Authentication** | **JWT** (access + refresh), stateless | `jjwt-*` trong `pom.xml`; `JwtAuthenticationFilter`, `AuthController`; `SecurityConfig` |
| **Authorization** | Role-based: `ADMIN`, `TOUR_GUIDE`, `TRAVELER` + `@PreAuthorize` / `hasRole` | `Server/BE/src/main/java/com/flourishtravel/config/RoleSeeder.java`; `SecurityConfig.java`; `TourOperationController` (`hasRole('ADMIN')`) |
| **Response wrapper** | `ApiResponse<T>` — `{ success, message?, data? }` | `Server/BE/src/main/java/com/flourishtravel/common/dto/ApiResponse.java` |
| **Global exception handling** | `@RestControllerAdvice` — map `ResourceNotFoundException`, `BadRequestException`, `ForbiddenException`, validation, generic 500 | `Server/BE/src/main/java/com/flourishtravel/common/exception/GlobalExceptionHandler.java` |
| **Validation** | Jakarta Validation (`@Valid`, `@Min`, …) trên controller/DTO; lỗi qua `MethodArgumentNotValidException` | Ví dụ: `BookingController.CreateBookingRequest`, `ReviewController`; handler tại `GlobalExceptionHandler` |
| **Scheduler / async** | `@EnableScheduling` + `@Scheduled` (Flora reminders); `@EnableAsync` (chưa thấy job queue riêng) | `FlourishTravelApplication.java`; `FloraReminderService.runReminderJob()` |
| **Queue / cron / Quartz** | **Không có Quartz/Rabbit/Kafka**; cron đơn giản qua Spring `@Scheduled` | `FloraReminderService.java` (`fixedRateString`) |
| **Logging** | SLF4J/Logback; level `com.flourishtravel: DEBUG` | `application.yml` → `logging.level` |
| **Swagger / OpenAPI** | **springdoc-openapi 2.4.0**; UI `/swagger-ui.html`, docs `/v3/api-docs` | `pom.xml`; `application.yml` → `springdoc.*`; `OpenApiConfig.java` |
| **Frontend framework** | **React 19** + **Vite 7** | `Client/FE/website/package.json` |
| **State management** | **React Context** (`AuthContext`) — không Redux/Zustand | `Client/FE/website/src/context/AuthContext.jsx` |
| **API client** | `fetch` + module `api/*`; base URL `VITE_API_URL` | `Client/FE/website/src/api/config.js`, `auth.js`, `chatbot.js`, `flora.js`, `users.js` |
| **UI / design system** | **Tailwind CSS 4** + **lucide-react** / **react-icons** | `package.json`; `vite.config.js` (`@tailwindcss/vite`) |
| **Mobile** | **Android Jetpack Compose** + Retrofit/Coil | `Mobile/mobile_app/` |

**Redis:** dependency `spring-boot-starter-data-redis` có trong `pom.xml` nhưng **không tìm thấy** `RedisTemplate` / cache annotation trong mã Java — có thể chưa dùng hoặc dùng cho tính năng khác ngoài phạm vi grep.

---

# 2. Xác minh API prefix

## Cấu hình backend

```yaml
server.servlet.context-path: /api
```

→ File: `Server/BE/src/main/resources/application.yml` (dòng `server.servlet.context-path`).

Controller map tương đối, ví dụ `@RequestMapping("/tours")` → endpoint thực: **`/api/tours`**.

## Swagger

`OpenApiConfig` khai báo server URL **`/api`** (relative) và `http://localhost:{port}/api` — file `Server/BE/src/main/java/com/flourishtravel/config/OpenApiConfig.java`.

## Frontend & mobile

| Client | Base URL | File |
|--------|----------|------|
| Web | `https://...ondigitalocean.app/api` (không trailing slash sau normalize) | `Client/FE/website/src/api/config.js`; deploy `Client/FE/.github/workflows/deploy.yml` set `VITE_API_URL=.../api` |
| Mobile | `BuildConfig.BASE_URL` = `{VITE_API_URL}/` từ `.env` | `Mobile/mobile_app/app/build.gradle.kts`; `RetrofitClient.kt` — path `"tours"` → `{BASE_URL}tours` |

## Kết luận

| Câu hỏi | Trả lời |
|---------|---------|
| Request đúng? | **`/api/tours`**, **`/api/chatbot/message`**, v.v. |
| Có dùng `/tours` không prefix? | **Không** (trừ khi reverse proxy strip `/api` — **không thấy** cấu hình strip trong repo FE deploy) |
| Rủi ro `/api/api/tours`? | **Có**, nếu client set `VITE_API_URL=https://host/api` **và** Retrofit/axios thêm path `/api/tours`. Hiện tại: FE gọi `` `${API_BASE}/chatbot/message` `` với `API_BASE` đã có `/api` → **đúng**. Mobile `BASE_URL` đã là `.../api/` + `"tours"` → **đúng**. **Rủi ro trung bình** nếu dev cấu hình `VITE_API_URL=https://host/api/api` hoặc copy nhầm từ Swagger server kép. |

---

# 3. Hồ sơ user & preference hiện có

## Entity / bảng

| Câu hỏi | Trả lời |
|---------|---------|
| Bảng preference riêng? | **Có** — `user_travel_preferences` | `UserTravelPreference.java` |
| User entity chứa travel prefs? | **Không** — prefs tách bảng `UserTravelPreference`; `User.java` chỉ profile + `marketingOptIn` |
| Budget / điểm đến / style / food / allergy / activity / notification? | **Có** (CSV/text hoặc boolean consent) trong `UserTravelPreference` |

### Trường `UserTravelPreference` (entity)

- `travelStyles`, `budgetLevel` (low/medium/high)
- `favoriteDestinations`, `favoriteFoods`, `foodDislikes`, `foodAllergies`
- `preferredActivities`, `avoidedActivities`, `travelPace`
- `travelingWithChildren`, `travelingWithElderly`
- `notificationConsent`, `locationConsent`, `personalizationConsent`

File: `Server/BE/src/main/java/com/flourishtravel/domain/flora/entity/UserTravelPreference.java`

## API profile

| Endpoint | Trả preference Flora? |
|----------|-------------------------|
| `GET /api/users/me` | **Không** — chỉ `id, email, fullName, phone, avatarUrl, role` (`UserProfileResponse.java`) |
| `PATCH /api/users/me` | **Không** — `UpdateProfileRequest` (profile cơ bản) |
| `GET /api/users/me/travel-preferences` | **Có** — `TravelPreferencesDto` |
| `PATCH /api/users/me/travel-preferences` | **Có** — `UpdateTravelPreferencesRequest` |
| `GET/PATCH /api/flora/preferences/me` | **Alias** cùng DTO | `FloraController.java` |

## Bảng đối chiếu Flora

| Yêu cầu Flora | Field/entity hiện có | Endpoint hiện có | Status | Khuyến nghị |
|---------------|---------------------|------------------|--------|-------------|
| Ngân sách (budget) | `budgetLevel` (low/medium/high) | `GET/PATCH .../travel-preferences` | **Reusable now** | Có thể map sang số VND ở tầng UI/LLM hint |
| Điểm đến yêu thích | `favoriteDestinations` (CSV) | Cùng endpoint | **Reusable now** | — |
| Phong cách du lịch | `travelStyles` | Cùng endpoint | **Reusable now** | — |
| Món thích / không thích / dị ứng | `favoriteFoods`, `foodDislikes`, `foodAllergies` | Cùng endpoint | **Reusable now** | — |
| Hoạt động ưa / tránh | `preferredActivities`, `avoidedActivities` | Cùng endpoint | **Reusable now** | — |
| Nhịp đi / đi cùng trẻ/em | `travelPace`, `travelingWithChildren`, `travelingWithElderly` | Cùng endpoint | **Reusable now** | — |
| Consent thông báo | `notificationConsent` | Cùng endpoint | **Reusable now** | Reminder job đã check |
| Consent vị trí | `locationConsent` | Cùng endpoint + `FloraPrivacyService.requireLocationConsent` | **Reusable now** | — |
| Consent cá nhân hóa AI | `personalizationConsent` | Cùng endpoint | **Reusable now** | Chatbot đã có `FloraPrivacyService.hasPersonalizationConsent` |
| Budget số cụ thể (min/max VND) | **Không** | — | **Missing** | Phase 2 hoặc derive từ `budgetLevel` |
| Notification settings chi tiết (channel, quiet hours) | **Không** | — | **Missing** | Phase 2 |
| Gộp prefs vào `GET /users/me` | **Không** | — | **Needs extension** | Optional embed để giảm round-trip FE |
| Lịch sử chat Flora | **Không** (chỉ `SearchLog` analytics) | — | **Missing** | Phase 2 nếu cần multi-turn server-side |

---

# 4. Kiến trúc Chatbot hiện tại

## Endpoint → implementation

| Endpoint | Controller | Service chính |
|----------|------------|---------------|
| `POST /api/chatbot/message` | `ChatbotController.java` | `ChatbotService.processMessage()` + `FloraContextBuilder.enrich()` |
| `GET /api/chatbot/nearby-places` | `ChatbotDataController.java` | `ChatbotDataService.getNearbyPlace()` |
| `GET /api/chatbot/weather-forecast` | `ChatbotDataController.java` | `ChatbotDataService.getWeatherForecast()` |
| `GET /api/chatbot/config` | `ChatbotConfigController.java` | `ChatbotConfigService.getFullConfig()` |
| `GET /api/chatbot/config/intents` | `ChatbotConfigController.java` | Cùng `getFullConfig()` (trả full config) |

## DTO

- **Request:** `ChatbotRequest.java` — `content`, `message` (alias), `sessionId`, `userId`, `state`, `bookingId`, `latitude`, `longitude`, `locale`, `source`
- **Response:** `ChatbotResponse.java` — `reply`, `answer`, `tours`, `quickReplies`, `state`, `suggestedActions`, `recommendations`, `warnings`, `nextMeeting`

## Intent / routing logic

1. **Training phrases** từ DB (`ChatbotTrainingPhrase`, import qua `/chatbot/config/import`) — match trước
2. **Rule-based:** policy FAQ, sentiment, tour card actions, keyword search
3. **LLM JSON intent** qua prompt template (`ChatbotService` + `FloraAiPersona.SYSTEM`)
4. **Fallback** khi LLM null

File: `ChatbotService.java` (~1200 dòng), `FloraAiPersona.java`

## LLM

| Hạng mục | Chi tiết |
|----------|----------|
| Tích hợp | **Có** — `LlmService.java` → **OpenRouter** (`/chat/completions`) |
| Model mặc định | `google/gemini-3-flash-preview` | `application.yml` → `app.openrouter.model` |
| API key | `OPENROUTER_API_KEY` env | `application.yml` |
| Khi không có key | `generate()` trả **null** → chatbot fallback rule/DB | `LlmService.java` |
| Prompt | `FloraAiPersona` + template JSON intent trong `ChatbotService` | |

## Rate limiting

**Đã triển khai (Phase 1.1):** in-memory rate limiter cho `POST /chatbot/message`, `GET /chatbot/nearby-places`, `GET /chatbot/weather-forecast`. Chi tiết: [`security-hardening.md`](./security-hardening.md). OpenRouter retry 429/503 trong `LlmService` vẫn giữ nguyên.

## Lịch sử chat

| Loại | Lưu trữ |
|------|---------|
| Multi-turn server session | **Không** — client gửi `state` Map qua request |
| Analytics | **Có** — `SearchLog` entity (query, sessionId, filters, resultCount) khi search tour | `SearchLog.java`, `ChatbotService.saveSearchLog()` |
| Group chat booking | **Có** — `Message` entity qua `/chat/bookings/{id}/messages` (khác Flora AI) |

## Trường mở rộng optional

**Có** — `bookingId`, `latitude`, `longitude`, `source`, `locale` trong `ChatbotRequest`; `FloraContextBuilder` đọc booking + prefs + location.

## Tương thích client cũ

**Có** — `content`/`reply` vẫn là contract chính; field Flora (`answer`, `suggestedActions`, …) **optional**. `POST /chatbot/message` **permitAll** (`SecurityConfig`) — JWT optional cho personalize.

## Bảng Flora ↔ Chatbot

| Yêu cầu Flora | Khả năng chatbot hiện tại | Reusable? | Thay đổi cần |
|---------------|---------------------------|-----------|--------------|
| Persona Flora tiếng Việt | `FloraAiPersona` | **Có** | Tinh chỉnh prompt |
| Gợi ý tour trong chat | `TourCard` + DB search | **Có** | — |
| Quick replies | `quickReplies` | **Có** | — |
| Context user đăng nhập | `ChatbotUserContextService` | **Có** | Cần JWT (optional hiện tại) |
| In-trip booking context | `bookingId` + `FloraContextBuilder` | **Có** | FE/mobile gửi `bookingId` |
| GPS trong message | Field có; enrich location | **Một phần** | Nearby vẫn theo `destination` string, chưa lat/lon trên `/nearby-places` |
| Journey / next meeting trong response | `nextMeeting`, `warnings` | **Có** | — |
| Lưu hội thoại đầy đủ | Không | **Không** | Entity + API nếu cần |
| Rate limit / abuse protection | In-memory per-instance | **Một phần** | Redis hoặc gateway khi scale ngang |
| Bắt buộc auth cho Flora | Hiện public | **Không an toàn** cho production | Cân nhắc auth hoặc anonymous quota |

---

# 5. Booking & ngữ cảnh tour đang diễn ra

## Nguồn dữ liệu đã kiểm tra

- Entity: `Booking`, `TourSession`, `TourItinerary`, `TourActivity`, `TourLocation` (lat/lon), `BookingGuest`
- API user: `GET /bookings/me`, `GET /bookings/{id}` → `UserBookingSummaryDto`, `UserBookingDetailDto`
- Chat context: `GET /chat/bookings/{bookingId}/context` → `TourChatContextDto`
- Flora journey: `GET /flora/bookings/{bookingId}/journey` → `FloraJourneyDto`
- Guide (HDV): `GET /guide/sessions`, `GET /guide/sessions/{id}`, `GET /guide/sessions/{sessionId}/members`
- Admin dispatch: `GET /tour-operations/sessions` — **chỉ ADMIN**

## Câu trả lời nghiệp vụ

| Câu hỏi | Trả lời |
|---------|---------|
| Xác định tour active cho booking? | **Có** — `FloraJourneyService.isActiveTrip()`, status `paid`/`confirmed`, ngày trong session | `FloraJourneyService.java` |
| Mục lịch trình tiếp theo? | **Một phần** — theo **ngày** (`TourItinerary.dayNumber`), không theo **activity** chi tiết + giờ | `FloraJourneyDto.currentScheduleItem` / `nextScheduleItem` |
| Giờ & điểm tập trung? | **Một phần** — `nextGatheringAt` = ngày session + giờ mặc định config (`FLORA_GATHERING_HOUR`); `meetingPoint` = `booking.pickupAddress` hoặc placeholder | `FloraJourneyService.computeNextGathering()` |
| Countdown? | **Có** — `minutesUntilGathering` / `FloraNextMeetingDto.minutesUntil` | |
| User sở hữu booking? | **Có** — `FloraPrivacyService.requireOwnedBooking`, `BookingService.getMyBookingDetail` | |
| Lộ dữ liệu khách khác? | **Chat/Flora user API không lộ** PII khách khác. **Guide API** (`/guide/sessions/.../members`) trả email/phone — **chỉ role TOUR_GUIDE**, không phải API Flora traveler | `GuideSessionMemberDto.java` |

## Bảng journey Flora

| Yêu cầu Flora | Nguồn hiện có | Endpoint/service | Status | Rủi ro bảo mật |
|---------------|---------------|------------------|--------|----------------|
| Active trip detection | Booking + session dates + status | `FloraJourneyService` | **Reusable now** | Thấp |
| Next schedule item (activity-level) | `TourActivity` có giờ/địa điểm nhưng Flora chưa map | DB tour | **Needs extension** | — |
| Meeting point chính xác GPS | `TourLocation` có lat/lon; Flora dùng text pickup + geocode | `FloraLocationService` | **Needs extension** | Geocode địa chỉ text không chính xác |
| Countdown tập trung | `minutesUntilGathering` | `GET /flora/.../journey` | **Reusable now** | Giờ tập trung **mặc định 07:00** có thể sai thực tế |
| Ownership check | `requireOwnedBooking` | Flora + Booking APIs | **Reusable now** | Thấp |
| Check-in / checkout realtime | Guide APIs | `/guide/.../check-in` | **Reusable now** (HDV) | Không expose cho traveler Flora trực tiếp |
| Chat nhóm đoàn | `ChatService` | `/chat/bookings/{id}/context` | **Reusable now** | Chỉ owner booking |

---

# 6. Hạ tầng thông báo (Notification)

## Thành phần

| Thành phần | File |
|------------|------|
| Entity | `Notification.java` — `type` (String), `title`, `body`, `data` (jsonb), `isRead` |
| Controller | `NotificationController.java` |
| Service | `NotificationService.java`; `createFloraNotification()` |
| Flora reminders | `FloraReminderService.java` + `FloraReminderDelivery` (idempotency) |
| Types | `FloraReminderTypes.java` (hằng số String, không enum DB) |

## API

- `GET /api/notifications?unread_only&limit`
- `PATCH /api/notifications/{id}/read`
- `POST /api/notifications/read-all`

## Câu trả lời

| Câu hỏi | Trả lời |
|---------|---------|
| BE tạo notification từ service? | **Có** — `NotificationService.createFloraNotification` |
| Scheduler 30/15/5 phút? | **Có** — `FloraReminderService` (`REMINDER_MINUTES = {30,15,5}`), job mỗi 60s (config `app.flora.reminder-check-ms`) |
| Idempotency / chống trùng? | **Có** — `flora_reminder_deliveries.idempotency_key` unique | `FloraReminderDelivery.java` |
| Timezone? | **Có** — `app.flora.timezone` (mặc định `Asia/Ho_Chi_Minh`); JPA `jdbc.time_zone: UTC` |
| Mở rộng type enum? | **Có** — `type` là String; thêm hằng trong `FloraReminderTypes` |
| Push notification (FCM/APNs)? | **Không** — chỉ in-app DB |
| Email/SMS reminder? | **Không** — `spring-boot-starter-mail` có nhưng reset password mail **commented** (`AuthService`) |
| Duplicate prevention toàn cục notification? | **Chỉ Flora reminders** — không có unique constraint chung trên `notifications` |

---

# 7. Địa điểm gần, bản đồ, thời tiết, nhà hàng

## API đã kiểm tra

| API | Implementation |
|-----|----------------|
| `GET /chatbot/nearby-places` | Open-Meteo **geocode** tên địa điểm + **Overpass/OSM** POI; fallback mock | `ChatbotDataService.java`, `OverpassClient`, `OpenMeteoClient` |
| `GET /chatbot/weather-forecast` | **Open-Meteo** forecast; fallback mock | `ChatbotDataService.java` |
| `GET /destinations/{slug}/map-stats` | Đếm POI tĩnh trong DB (`Destination.mapPois`) | `DestinationService.mapStats()` |
| `POST /destinations/flora-match` | **Rule-based** % match từ `floraMatchDefault` + prefs | `DestinationService.floraMatch()` |
| `POST /catalog/flora-recommend` | Gợi ý tour từ catalog logic | `CatalogService.floraRecommend()` |
| `POST /flora/bookings/{id}/location` | GPS thật từ client; haversine tới geocode meeting point | `FloraLocationService.java` |

## Câu trả lời

| Câu hỏi | Trả lời |
|---------|---------|
| GPS thật? | **Có** cho Flora location ping (cần consent); **Không** cho `/nearby-places` (chỉ query `destination` string) |
| Tính khoảng cách? | **Có** — haversine trong `FloraLocationService` |
| Tính route / chỉ đường? | **Không** |
| Nearby places thật? | **Một phần** — OSM Overpass khi geocode OK; else mock |
| Dữ liệu nhà hàng? | **Qua OSM tags** hoặc mock; không có Google Places / dedicated restaurant API |
| Thời tiết thật? | **Có** (Open-Meteo) với fallback mock |
| Tọa độ destination / meeting? | `TourLocation` có lat/lon; `Destination` có `mapPois`; meeting point thường **text** `pickupAddress` |
| API keys | `OPENROUTER_API_KEY` (LLM); `GOOGLE_MAPS_API_KEY` trong `application.yml` nhưng **không thấy** code Java đọc key này |
| Rate limit bên thứ ba | Không quản lý tập trung; Overpass/Open-Meteo gọi trực tiếp |

---

# 8. Privacy, consent & security audit

## Đã có

- Consent flags: `locationConsent`, `notificationConsent`, `personalizationConsent` (`UserTravelPreference`)
- `FloraPrivacyService` — enforce location; sanitize phone/email trong prompt
- Booking ownership — `requireOwnedBooking`, chat context check `userId`
- JWT stateless; admin/guide route separation (`SecurityConfig`)
- PII encryption key config `app.encryption.pii-key` (cần env production)
- Admin audit log API — `AdminAuditLogController` (admin only)
- Location retention — `app.flora.location-retention-hours` + cleanup job

## Khoảng trống (theo mức độ)

| Gap | Mức | Chi tiết |
|-----|-----|----------|
| `POST /chatbot/message` **public** không bắt buộc auth | **Critical** | Abuse LLM cost, không audit user | `SecurityConfig` |
| Không rate limit chatbot/IP | **High** | Dễ spam OpenRouter |
| GPS gửi qua chat body không validate consent ở mọi path | **High** | Cần audit mọi entry dùng lat/lon |
| Không push notification — reminder chỉ in-app | **Medium** | User không mở app sẽ miss |
| `GOOGLE_MAPS_API_KEY` config nhưng unused — rủi ro lộ key vô ích | **Low** | `application.yml` |
| Không có data retention policy document trong code cho `SearchLog` | **Medium** | Analytics chat |
| Hibernate `ddl-auto: update` production | **High** | Không migration có kiểm soát |
| Guide members API lộ email/phone | **Medium** (đúng vai trò HDV) | Không dùng cho Flora traveler |
| LLM prompt có user context — phụ thuộc `sanitizeForPrompt` | **Medium** | Cần review định kỳ |
| Không có explicit AI consent UI trên mobile (một phần có FE `PrivacySettings`) | **Medium** | `PrivacySettings.jsx` web; mobile chưa đồng bộ đầy đủ |

---

# 9. Kiểm toán tích hợp Frontend / Mobile

## Web (`Client/FE/website`)

| Màn hình / module | File |
|-------------------|------|
| Chat Flora global | `src/components/FloatingChatbot.jsx` |
| Flora trên booking detail | `src/components/FloraCompanion.jsx` |
| Privacy / prefs | `src/pages/user/PrivacySettings.jsx` |
| API chatbot | `src/api/chatbot.js` |
| API flora + notifications | `src/api/flora.js` |
| API user prefs | `src/api/users.js` |
| Booking detail | `src/pages/user/BookingDetail.jsx` |
| My trips | `src/pages/user/MyJourney.jsx` |
| Tour detail / listing | `TourDetail.jsx`, `TourListing.jsx` |
| Group chat | `GroupChat.jsx` |
| Auth storage | `src/api/auth.js` + `AuthContext.jsx` (localStorage tokens) |
| Routes | `src/App.jsx` — `FloraGlobalAssistant` exclude admin/guide/group chat |

**Maps:** không thấy Google Maps embed dedicated; destination dùng dữ liệu tĩnh/API.

## Mobile (`Mobile/mobile_app`)

| Màn hình | File |
|----------|------|
| Flora AI chat | `ui/screens/AssistantScreen.kt` |
| Chuyến đi / bookings | `ui/screens/ScheduleScreen.kt` |
| Tour detail + book | `TourDetailScreen.kt`, booking flow screens |
| API | `RetrofitClient.kt`, `ChatbotApiService.kt`, `BookingApiService.kt` |
| Auth | `SessionManager.kt` |

## File nên chỉnh khi triển khai Flora (gợi ý)

**Web:** `FloatingChatbot.jsx`, `FloraCompanion.jsx`, `PrivacySettings.jsx`, `api/chatbot.js`, `api/flora.js`, `api/users.js`, `BookingDetail.jsx`, `MyJourney.jsx`, `App.jsx`

**Mobile:** `AssistantScreen.kt`, `ChatbotModels.kt`, `ScheduleScreen.kt`, `BookingModels.kt`, `MainActivity.kt`, (mới) prefs screen nếu cần

---

# 10. Ma trận tái sử dụng API

| API endpoint | Mục đích Flora AI | Tái sử dụng trực tiếp? | Dữ liệu thiếu | Mở rộng cần |
|--------------|-------------------|------------------------|---------------|-------------|
| `GET /users/me` | Profile cơ bản | **Có** | Travel prefs | Optional embed prefs |
| `GET/PATCH /users/me/travel-preferences` | Personalization | **Có** | Budget số | — |
| `GET /tours` | Gợi ý catalog | **Có** | — | — |
| `GET /tours/{id}` | Chi tiết tour card | **Có** | — | — |
| `GET /tours/{id}/similar` | Cross-sell | **Có** | — | — |
| `POST /destinations/flora-match` | % phù hợp điểm đến | **Có** (rule-based) | ML thật | Logic hoặc LLM |
| `POST /catalog/flora-recommend` | Gợi ý catalog | **Có** | — | — |
| `GET /bookings/me` | Chuyến của tôi | **Có** | Activity-level journey | Mobile DTO đã fix mapping |
| `GET /bookings/{id}` | Chi tiết đơn in-trip | **Có** | — | — |
| `POST /planner/generate` | Lịch trình tự do | **Có** | Liên kết booking | — |
| `GET /notifications` | Trung tâm thông báo | **Có** | Push realtime | FCM Phase 2 |
| `GET /chat/bookings/{id}/context` | Ngữ cảnh chat đoàn | **Có** | — | — |
| `POST /chatbot/message` | Core Flora chat | **Có** (đã mở rộng) | Server-side history | Redis rate limit multi-instance |
| `GET /chatbot/nearby-places` | POI gợi ý | **Một phần** | lat/lon query | Thêm query GPS |
| `GET /chatbot/weather-forecast` | Thời tiết | **Có** (Open-Meteo) | Theo GPS chính xác | Optional lat/lon |
| `GET/POST/DELETE /favorites` | Sở thích ngầm | **Có** | — | — |
| `POST /reviews` | Feedback sau tour | **Có** | — | Link Flora post-tour reminder |
| `GET /guide/sessions` | HDV (không Flora traveler) | **Không** (role) | — | — |
| `GET /tour-operations/sessions` | Admin dispatch | **Không** (ADMIN) | — | — |
| `GET /flora/bookings/{id}/journey` | Journey panel | **Có** | Activity giờ cụ thể | Itinerary activity mapping |
| `POST /flora/bookings/{id}/location` | GPS in-trip | **Có** | Route navigation | Maps Phase 2 |
| `GET /flora/preferences/me` | Alias prefs | **Có** | — | — |

---

# 11. Phân tích khoảng trống Flora AI MVP

> **Ghi chú:** Nhiều mục Phase 1 **đã được implement một phần** trong backend (`domain.flora`). Phần dưới phân biệt *đã có*, *cần hoàn thiện*, và *chưa nên làm*.

## Reuse without modification

- `POST /api/chatbot/message` (contract `content`/`reply` + optional Flora fields)
- `GET/PATCH /api/users/me/travel-preferences` và `/api/flora/preferences/me`
- `GET /api/bookings/me`, `GET /api/bookings/{id}`
- `GET /api/flora/bookings/{bookingId}/journey`
- `GET /api/tours`, `GET /api/tours/{id}`, `GET /api/tours/{id}/similar`
- `POST /api/catalog/flora-recommend`, `POST /api/destinations/flora-match`
- `GET /api/chatbot/weather-forecast` (Open-Meteo + fallback)
- `GET /api/notifications`, `PATCH .../read`, `POST .../read-all`
- `GET /api/chat/bookings/{bookingId}/context`
- `GET/POST /api/favorites`
- `ChatbotUserContextService`, `FloraAiPersona`, `LlmService` (khi có `OPENROUTER_API_KEY`)
- `FloraReminderService` + `FloraReminderDelivery` (in-app reminders)

## Extend existing code

- `ChatbotDataService` / `GET /chatbot/nearby-places` — thêm `lat`, `lon`, `radius`
- `FloraJourneyService` — map `TourActivity` (giờ, địa điểm, lat/lon) thay vì chỉ `TourItinerary` theo ngày
- `SecurityConfig` — policy auth/rate limit cho `/chatbot/message`
- `NotificationService` — chuẩn hóa `type` enum; webhook push (FCM) sau
- `GET /users/me` — optional nested `travelPreferences`
- Mobile `BookingSummary` DTO (đã cần khớp `UserBookingSummaryDto` — kiểm tra sau mỗi BE change)
- FE `PrivacySettings.jsx` — đồng bộ consent với mobile
- `ChatbotService` — persist conversation nếu product yêu cầu

## New backend code actually required

*(Tối thiểu nếu coi MVP backend Flora đã merge — chỉ còn production hardening)*

- **Rate limiting** — **đã có** (in-memory; xem `security-hardening.md`); Redis/gateway khi multi-instance
- **Push notification provider** (FCM) — **chưa có**
- **Flyway/Liquibase migrations** thay `ddl-auto: update` — **khuyến nghị mạnh**, chưa có
- **Chat history entity** (nếu product bắt buộc server-side multi-turn) — **chưa có**
- **Google Maps Directions** (nếu cần chỉ đường thật) — config key có nhưng **chưa tích hợp**

## Do not build in Phase 1

- GPS tracking liên tục / background location
- Live map turn-by-turn routing
- Restaurant API thương mại (Yelp/Google Places billing) — dùng OSM + rule đủ MVP
- Push notification đa nền tảng (nếu chưa có FCM credentials)
- Real-time websocket Flora chat (đã có WS cho chat nhóm khác — không trộn Phase 1)
- Thay thế hoàn toàn rule-based chatbot bằng LLM-only
- `tour-operations` cho end-user Flora

## Recommended Flora AI Phase 1 scope

1. **Chat Flora** với persona + tour search + user prefs (JWT khuyến khích, không bắt buộc guest)
2. **Travel preferences UI** (web đã có; mobile cần màn consent/prefs)
3. **Journey panel** trên booking detail (`FloraCompanion` + `GET /flora/.../journey`)
4. **In-app reminders** 30/15/5 phút (đã có job — cần user bật `notificationConsent`)
5. **Location ping thủ công** khi user consent (`POST /flora/.../location`) — không background tracking
6. **Open-Meteo weather** + OSM nearby trong chat
7. **Không** push, không maps routing, không lưu full chat server-side

## Recommended implementation order (file-level)

1. `SecurityConfig.java` — rate limit / auth policy cho chatbot (production)
2. `Client/FE/website/src/pages/user/PrivacySettings.jsx` + `api/users.js` — verify E2E prefs
3. `Client/FE/website/src/components/FloatingChatbot.jsx` + `api/chatbot.js` — `bookingId`, Bearer, `source`
4. `Client/FE/website/src/components/FloraCompanion.jsx` + `api/flora.js` — journey + location button
5. `Mobile/.../AssistantScreen.kt` + `ChatbotModels.kt` — parity web chat
6. `Mobile/.../ScheduleScreen.kt` + `BookingModels.kt` — bookings list khớp BE DTO
7. `FloraJourneyService.java` — activity-level schedule (nếu product cần chính xác giờ)
8. `ChatbotDataController.java` + `ChatbotDataService.java` — nearby by lat/lon
9. `NotificationService` + FCM adapter (Phase 1.5 / 2)
10. `db/migration/*` — Flyway thay `ddl-auto: update`

---

## Tóm tắt cho Cursor chat

1. **Số API tái sử dụng trực tiếp:** **~18** endpoint lõi (users prefs, tours, bookings, chatbot, flora journey/location, notifications, favorites, chat context, catalog/destination flora, planner, reviews).
2. **Số API cần mở rộng:** **~6** (`nearby-places`, `users/me`, journey activity-level, chatbot auth/rate limit, notifications push, optional chat history).
3. **Số API/backend mới thực sự thiếu:** **~4** cụm (rate limit, push FCM, chat history server-side nếu bắt buộc, maps routing) — phần lớn Flora domain **đã tồn tại**.
4. **Rủi ro kỹ thuật lớn nhất:** `POST /chatbot/message` **public** + **không rate limit** + phụ thuộc **OPENROUTER_API_KEY**; schema **Hibernate auto-update** trên production.
5. **Rủi ro privacy lớn nhất:** Location & personalization dựa consent DB nhưng **endpoint chatbot public**; GPS/geocode meeting point **không chính xác** nếu chỉ dùng text address.
6. **Nhiệm vụ implement đầu tiên khuyến nghị:** **Production hardening** — `SecurityConfig.java` (auth/rate limit cho chatbot) + xác nhận E2E `PrivacySettings` / mobile prefs trước khi bật location reminders rộng rãi.

---

*Tài liệu được tạo bởi kiểm toán read-only — không thay đổi mã nguồn.*

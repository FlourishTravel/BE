# Flora AI Phase 1.7A — Checklist FCM staging

**Ngày kiểm toán:** 2026-06-23  
**Phạm vi:** Kiểm tra code Android + backend FCM thực tế; không bật FCM trên production trong phase này.

---

## 1. Tóm tắt tích hợp FCM (từ codebase)

### 1.1 Backend

| Thành phần | Hành vi thực tế |
|------------|-----------------|
| **Firebase Admin SDK** | `firebase-admin` 9.2.0 trong `pom.xml`; `FirebaseFcmPushSender` khởi tạo từ file credentials |
| **`FCM_ENABLED`** | `app.push.fcm.enabled` ← env `FCM_ENABLED`; **mặc định `false`** |
| **`GOOGLE_APPLICATION_CREDENTIALS`** | Map tới `app.push.fcm.credentials-path`; đọc file JSON service account qua `FileInputStream` |
| **`PII_ENCRYPTION_KEY`** | `app.encryption.pii-key`; mã hóa `push_devices.token_ciphertext` và `booking_guests.id_number`; **thiếu key = plaintext** |
| **Bean sender** | `FCM_ENABLED=false` → `NoOpFcmPushSender`; `true` + credentials hợp lệ → `FirebaseFcmPushSender` |
| **PushDevice lifecycle** | Register (hash token SHA-256, encrypt ciphertext), cap 5 thiết bị/user, deactivate on invalid token / unregister / cap eviction |
| **PushNotificationDelivery lifecycle** | Queue sau commit in-app notification → dispatcher `@Scheduled` mỗi 60s → PENDING/SENT/FAILED/INVALID_TOKEN/SKIPPED |
| **Allow-list loại push** | `TOUR_REMINDER_30/15/5_MINUTES`, `RETURN_TO_BUS_ALERT`, `SCHEDULE_CHANGED`, `POST_TOUR_FEEDBACK` |
| **Retry** | `FCM_MAX_ATTEMPTS` (default 3); backoff `60s * attempt_count` |
| **Invalid token** | `UNREGISTERED` / `INVALID_ARGUMENT` → deactivate device, status `INVALID_TOKEN` |
| **In-app khi FCM lỗi** | `NotificationService.createFloraNotification()` **luôn** lưu `notifications` trước; queue push **sau commit**, độc lập |
| **Consent** | `FloraPrivacyService.hasNotificationConsent()` — queue và dispatcher đều kiểm tra |
| **Nội dung push** | `PushContentSanitizer` — **không** dùng title/body in-app; chỉ text generic |
| **FCM data payload** | Chỉ `notificationId`, `target=FLORA_NOTIFICATION` — **không** bookingId trên lock screen |
| **Logging** | `event=push_sent|push_failed|push_invalid_token` — **không** log raw token trong dispatcher |

**API:**
- `POST /api/notifications/push-devices`
- `POST /api/notifications/push-devices/unregister`
- `GET /api/notifications/push-devices/status`

**Production hiện tại (`.do/app.yaml`):** Chỉ có `DB_PASSWORD`, `JWT_SECRET` — **chưa** cấu hình `FCM_ENABLED`, `GOOGLE_APPLICATION_CREDENTIALS`, `PII_ENCRYPTION_KEY`.

### 1.2 Android (`com.example.flourishtavelapp`)

| Thành phần | Hành vi thực tế |
|------------|-----------------|
| **google-services.json** | Gitignored; `app/build.gradle.kts` chỉ apply plugin `com.google.gms.google-services` **nếu file tồn tại** |
| **Package** | `applicationId = "com.example.flourishtavelapp"` |
| **POST_NOTIFICATIONS** | Khai báo manifest; **chỉ** request qua `FloraSettingsScreen` khi user bấm bật thông báo thiết bị |
| **MainActivity** | Test `DevicePushStartupPolicyTest` xác nhận **không** request permission lúc startup |
| **FloraFirebaseMessagingService** | `onMessageReceived` hiển thị notification local; `onNewToken` lưu token nếu logged in — sync khi mở settings |
| **Notification channel** | `flora_journey_reminders` — "Flora – Nhắc hành trình", `IMPORTANCE_DEFAULT` |
| **Token sync** | `PushTokenRepository.syncIfEligible` — cần consent + Android permission |
| **Logout** | `LogoutCoordinator` → unregister token API → logout → clear session |
| **OkHttp logs** | Redact `"token"` và `Bearer` trong `RetrofitClient` |

---

## 2. Checklist Firebase Console

- [ ] Tạo hoặc chọn Firebase project cho staging
- [ ] Đăng ký ứng dụng Android package **`com.example.flourishtavelapp`**
- [ ] Tải `google-services.json` — **chỉ lưu local / secret store**, không commit
- [ ] Xác nhận yêu cầu SHA-1/SHA-256 (debug keystore cho staging; release keystore cho production)
- [ ] Tạo service account Firebase Admin (JSON) cho backend staging
- [ ] Lưu service account **ngoài Git** (DO Secrets / vault / file mount container)
- [ ] Tắt hoặc hạn chế quyền service account tối thiểu (Firebase Cloud Messaging)

---

## 3. Checklist Android staging build

- [ ] `app/google-services.json` có mặt trên máy build staging (file bị ignore bởi Git — đúng)
- [ ] Package Android khớp Firebase application package (`com.example.flourishtavelapp`)
- [ ] `assembleDebug` hoặc staging build thành công **sau khi** thêm `google-services.json`
- [ ] `POST_NOTIFICATIONS` **chỉ** được request sau thao tác user trên Flora Settings (không lúc mở app)
- [ ] User bật **Flora notification consent** (`user_travel_preferences.notification_consent = true`)
- [ ] User bật quyền thông báo Android (Settings → bật thông báo thiết bị)
- [ ] Đăng ký FCM token thành công — `GET /api/notifications/push-devices/status` → `pushEnabled=true` (khi BE FCM bật)
- [ ] Kênh thông báo **"Flora – Nhắc hành trình"** xuất hiện trong cài đặt Android
- [ ] Nhận push trên thiết bị thật — title/body generic (không lộ địa điểm/tên)
- [ ] Chạm notification mở app an toàn (không crash)
- [ ] App refetch dữ liệu notification qua API đã xác thực (in-app list / Flora journey)
- [ ] Logcat: không thấy raw FCM token trong OkHttp (phải `[redacted]`)

---

## 4. Checklist Backend staging

- [ ] `FCM_ENABLED=true` trên staging only
- [ ] `GOOGLE_APPLICATION_CREDENTIALS` trỏ file service account (mount an toàn, không trong image Docker)
- [ ] `PII_ENCRYPTION_KEY` set (base64 32 bytes) — **bắt buộc** trước khi lưu token production
- [ ] Log startup: `FCM push sender initialized` (không phải `push sender inactive`)
- [ ] Dispatcher chạy (`FCM_DISPATCHER_POLL_MS` default 60000ms)
- [ ] Log không chứa raw FCM token
- [ ] Repository / container image không chứa Firebase credentials
- [ ] Tạo Flora in-app notification khi FCM tắt hoặc lỗi — bản ghi `notifications` vẫn có
- [ ] Token invalid (gỡ app / revoke) → `push_devices.active=false`
- [ ] Sau 3 lần thất bại tạm thời → `push_notification_deliveries.status=FAILED`
- [ ] `notificationConsent=false` → không queue delivery (`event=push_skipped_consent`)
- [ ] Smoke: reminder hoặc `SCHEDULE_CHANGED` tạo cả in-app + push queue

---

## 5. Privacy validation (lock screen & payload)

Kiểm tra trên **thiết bị thật** với màn hình khóa:

- [ ] Thông báo lock-screen chỉ hiển thị **text generic** (từ `PushContentSanitizer`)
- [ ] **Không** có booking ID trong text hiển thị
- [ ] **Không** có điểm tập trung / meeting point trong text hiển thị
- [ ] **Không** có tên hành khách trong text hiển thị
- [ ] **Không** có thông tin thanh toán / hoàn tiền trong text hiển thị
- [ ] **Không** có tọa độ / địa chỉ trong text hiển thị
- [ ] FCM data payload chỉ chứa `notificationId` + `target` — chi tiết lấy qua API sau khi mở app
- [ ] In-app `notifications.data` có thể chứa `bookingId` — **chỉ** trong app đã đăng nhập, không trên push banner

---

## 6. Kịch bản test staging đề xuất

| # | Kịch bản | Kỳ vọng |
|---|----------|---------|
| 1 | FCM tắt, tạo reminder Flora | In-app có; không có push |
| 2 | FCM bật, consent on, permission on | In-app + push generic |
| 3 | FCM bật, consent off | In-app có; push skipped |
| 4 | Logout | Unregister API gọi; token local xóa |
| 5 | Token refresh (`onNewToken`) | Token lưu local; sync khi mở Flora Settings |
| 6 | Gỡ cài đặt app / token hết hạn | Device deactivated sau send fail |

---

## 7. Phân loại phát hiện FCM

| Mức | Phát hiện |
|-----|-----------|
| **BLOCKER** | Production chưa cấu hình `FCM_ENABLED`, credentials, `PII_ENCRYPTION_KEY` |
| **BLOCKER** | `google-services.json` thiếu cho build release/staging thật |
| **HIGH RISK** | Chưa có bằng chứng test push trên **thiết bị Android thật** |
| **HIGH RISK** | Thiếu `PII_ENCRYPTION_KEY` → FCM token lưu plaintext DB |
| **MEDIUM RISK** | Không có monitoring/alert cho `push_notification_deliveries` FAILED |
| **MEDIUM RISK** | Dispatcher poll 60s — độ trễ push có thể lớn |
| **LOW RISK** | `onNewToken` không sync ngay lập tức (chờ user mở settings) |
| **READY** | Code FCM + sanitizer + consent + unit tests (112 BE tests) |
| **READY** | In-app notification tách khỏi FCM delivery path |

# Flora AI Phase 1.6 — FCM Push Audit

Date: 2026-06-23. Schema managed via Hibernate `ddl-auto: update`.

## 1. Firebase Cloud Messaging configured?

**No.** Backend has no `firebase-admin` dependency. Android has no `firebase-messaging` dependency.

## 2. Android Firebase Messaging?

**No.** No `FirebaseMessagingService`, no FCM token registration.

## 3. `google-services.json`

| Item | Status |
|------|--------|
| Local file | **Missing** in repo |
| Git ignore | **Not listed** — added in Phase 1.6 to `.gitignore` |
| Setup | Developers copy from Firebase Console locally |

## 4. Backend Firebase Admin SDK / push provider?

**None.** In-app notifications only via `NotificationService.createFloraNotification()`.

## 5. In-app notification creation

`NotificationService.createFloraNotification(userId, type, title, body, bookingId)` persists `notifications` row with optional `data` JSON `{"bookingId":"..."}`.

Callers: `FloraReminderService`, `FloraScheduleChangeNotifier`.

Clients poll `GET /api/notifications`.

## 6. Flora types appropriate for push

| Type | Push candidate |
|------|----------------|
| `TOUR_REMINDER_30_MINUTES` | Yes |
| `TOUR_REMINDER_15_MINUTES` | Yes |
| `TOUR_REMINDER_5_MINUTES` | Yes |
| `RETURN_TO_BUS_ALERT` | Yes |
| `SCHEDULE_CHANGED` | Yes (published only) |
| `POST_TOUR_FEEDBACK` | Yes |
| `MEETING_POINT_REMINDER` | No sender yet |
| `NEARBY_ATTRACTION` | No |
| `CHECKIN_REMINDER` | No |

## 7. Consent before notification

`FloraPrivacyService.hasNotificationConsent(userId)` — opt-out default `true` when no prefs row.

Checked in `FloraReminderService`, `FloraScheduleChangeNotifier`, `FloraLocationService` (return-to-bus) **before** creating in-app notification.

Push layer re-checks consent at queue and dispatch time.

## 8. Android 13+ `POST_NOTIFICATIONS`

**Not declared** before Phase 1.6. Added in implementation with explicit user-action request only.

## 9. Android notification channel

**None** before Phase 1.6. Added `Flora – Nhắc hành trình` channel.

## 10. Encryption for FCM tokens

`PiiEncryptionConverter` (AES-256-GCM, `app.encryption.pii-key`) exists for CCCD. **Reused** for FCM token ciphertext on `PushDevice.tokenCiphertext`. `tokenHash` (SHA-256) for lookup without decrypting.

If `PII_ENCRYPTION_KEY` absent, tokens stored with converter fallback (documented risk in `fcm-push-notifications.md`).

## 11. Async / scheduled infrastructure

`@EnableScheduling` + `FloraReminderService.runReminderJob()`. `@EnableAsync` present but unused.

Push dispatcher uses `@Scheduled` poll (same style as reminders).

## 12. Risks

| Risk | Mitigation in 1.6 |
|------|-------------------|
| Duplicate push | Unique `(notificationId, pushDeviceId)` on `push_notification_deliveries` |
| Token rotation | Re-register upserts by `tokenHash`; reassigns token from prior user |
| Logout | `POST /notifications/push-devices/unregister` + local token clear |
| Multi-device | Multiple `PushDevice` rows per user; cap configurable |
| Lock-screen privacy | Generic push copy only; payload `notificationId` + `target` |
| FCM failure | In-app notification always created first; push failures logged safely |
| Draft schedule | No push for drafts (schedule push only from published notifier path) |

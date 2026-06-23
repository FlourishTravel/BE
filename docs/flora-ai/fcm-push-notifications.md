# Flora AI Phase 1.6 — FCM push notifications

## Architecture

| Layer | Role |
|-------|------|
| **In-app `Notification`** | Canonical record in PostgreSQL; web + mobile poll `GET /api/notifications` |
| **`PushNotificationDelivery`** | Per-device FCM dispatch queue with idempotency |
| **FCM** | Optional Android delivery when consent, permission, and credentials are satisfied |

Push failure never blocks in-app notification creation.

## Firebase setup (no secrets in Git)

### Android

1. Create a Firebase project and add Android app with package `com.example.flourishtavelapp`.
2. Download `google-services.json` from Firebase Console.
3. Copy to `Mobile/mobile_app/app/google-services.json` (file is **gitignored**).
4. Use `app/google-services.json.example` as a template reference only.

### Backend

1. Firebase Console → Project settings → Service accounts → Generate new private key.
2. Store JSON outside the repo (e.g. secrets manager or secure server path).
3. Set environment variables:

```bash
FCM_ENABLED=true
GOOGLE_APPLICATION_CREDENTIALS=/secure/path/firebase-service-account.json
PII_ENCRYPTION_KEY=<base64-32-bytes>   # recommended for token ciphertext
```

Optional:

```bash
FCM_MAX_DEVICES_PER_USER=5
FCM_MAX_ATTEMPTS=3
FCM_DISPATCHER_POLL_MS=60000
```

## API endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/notifications/push-devices` | JWT |
| POST | `/api/notifications/push-devices/unregister` | JWT |
| GET | `/api/notifications/push-devices/status` | JWT |

Responses never include raw FCM tokens.

## Allowed push types (configurable)

- `TOUR_REMINDER_30_MINUTES`
- `TOUR_REMINDER_15_MINUTES`
- `TOUR_REMINDER_5_MINUTES`
- `RETURN_TO_BUS_ALERT`
- `SCHEDULE_CHANGED`
- `POST_TOUR_FEEDBACK`

## Privacy

Lock-screen title/body use generic Vietnamese copy via `PushContentSanitizer`. FCM data payload:

```json
{ "notificationId": "<uuid>", "target": "FLORA_NOTIFICATION" }
```

No booking IDs, locations, names, or payment data in push content.

## Consent vs Android permission

| Control | Effect |
|---------|--------|
| Flora `notificationConsent` (backend) | Required for in-app Flora notifications and push queue |
| Android `POST_NOTIFICATIONS` | Required for device push; requested only after user taps **Bật thông báo trên thiết bị** |

## Token lifecycle

| Event | Behavior |
|-------|----------|
| Login + consent + permission | Register FCM token |
| Token refresh | Saved locally; sync on next eligible action |
| Consent off (save settings) | Unregister token |
| Logout | Unregister then clear session |
| Same token, new user | Device row reassigned to new user |
| Invalid FCM token | Device deactivated, delivery `INVALID_TOKEN` |

## Retry

Pending deliveries retry with bounded attempts (`FCM_MAX_ATTEMPTS`). Invalid tokens are not retried.

## Web

Web remains **in-app only** — no browser push in Phase 1.6.

## Known limitations

- iOS, SMS, email, browser push not implemented.
- FCM disabled by default; production must set credentials explicitly.
- Without `PII_ENCRYPTION_KEY`, token storage uses converter fallback (see audit).
- Real FCM delivery requires valid `google-services.json` on device builds.

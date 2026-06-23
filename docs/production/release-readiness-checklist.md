# Flora AI Phase 1.7A — Checklist sẵn sàng release production

**Ngày kiểm toán:** 2026-06-23  
**Phạm vi:** Flora AI Phase 1.0–1.6 + hạ tầng deploy hiện có.  
**Ghi chú:** Đánh dấu `[x]` chỉ sau khi **đã thực hiện và xác minh** — không đoán từ code alone.

---

## Backend code readiness

- [ ] `mvn -B test` pass (CI: `.github/workflows/ci.yml`)
- [ ] Không có thay đổi breaking API chưa document
- [ ] Profile `cloud`: seed tắt (`app.seed.enabled=false`)
- [ ] OpenAPI tắt trên production (`application-cloud.yml`)
- [ ] Chatbot rate limiting hoạt động (auth + anonymous limits)
- [ ] Flora journey, schedule override, feedback APIs hoạt động trên staging
- [ ] Push APIs (`/notifications/push-devices/*`) deploy cùng release

---

## Android code readiness

- [ ] `./gradlew test` pass
- [ ] `./gradlew assembleDebug` pass
- [ ] Package `com.example.flourishtavelapp` ổn định
- [ ] Flora Settings: consent + permission flow đúng spec
- [ ] GPS foreground chỉ khi `locationConsent=true`
- [ ] `LogoutCoordinator` unregister token trước clear session
- [ ] OkHttp redact token / GPS / Bearer trong logs

---

## Database readiness

- [ ] Xác nhận 52 bảng entity tồn tại trên production (hoặc staging mirror)
- [ ] Bảng Flora mới có mặt: `user_travel_preferences`, `flora_reminder_deliveries`, `user_location_pings`, `tour_session_activity_overrides`, `push_devices`, `push_notification_deliveries`
- [ ] Cột `reviews.feedback_tags` tồn tại
- [ ] Cột Flora trên `tour_activities` tồn tại (`is_gathering_event`, `gathering_event_type`, `schedule_status`, `location_address`)
- [ ] `notifications.data` kiểu JSONB trên PostgreSQL
- [ ] **Verify database backup completed before migration** (hoặc trước deploy schema change)
- [ ] Không chạy destructive DDL trên production

---

## Flyway readiness

- [ ] Quyết định Phase 1.7B: baseline strategy đã review (`docs/production/flyway-migration-audit.md`)
- [ ] Staging DB copy đã test `validate` hoặc baseline Flyway
- [ ] Kế hoạch tắt `ddl-auto: update` trên production đã approved
- [ ] `TourCatalogSchemaPatch` đã có kế hoạch thay thế bằng migration
- [ ] Rollback playbook có owner

---

## FCM readiness

- [ ] Hoàn thành `docs/production/fcm-staging-checklist.md`
- [ ] Firebase project + Android app đăng ký
- [ ] Service account backend staging tested
- [ ] **Run FCM real-device smoke test**
- [ ] **Verify FCM failure does not block in-app notification**
- [ ] Production secrets: `FCM_ENABLED`, `GOOGLE_APPLICATION_CREDENTIALS`, `PII_ENCRYPTION_KEY`

---

## Secrets and environment variables

### Bắt buộc production (đã có trên `.do/app.yaml`)

- [ ] `DB_PASSWORD` (SECRET)
- [ ] `JWT_SECRET` (SECRET) — **không** dùng default trong `application.yml`

### Khuyến nghị / bắt buộc tùy tính năng

- [ ] `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_SSL_MODE=require`
- [ ] `PII_ENCRYPTION_KEY` (CCCD + FCM token encryption)
- [ ] `OPENROUTER_API_KEY` (Flora chatbot)
- [ ] `FRONTEND_URL`, `API_BASE_URL`
- [ ] `FCM_ENABLED`, `GOOGLE_APPLICATION_CREDENTIALS` (nếu bật push)
- [ ] `MOMO_*` production credentials (thay sandbox default)
- [ ] `S3_UPLOAD_*` nếu `UPLOAD_STORAGE=s3`
- [ ] `GOOGLE_MAPS_API_KEY` nếu dùng maps server-side

### Không commit

- [ ] `.env`, `env`, `google-services.json`, Firebase service account JSON

---

## Privacy and consent validation

- [ ] **Verify notificationConsent false blocks Flora push** (queue + dispatcher)
- [ ] **Verify locationConsent false blocks GPS use** (mobile + API reject)
- [ ] **Verify personalizationConsent false blocks preference-learning suggestions**
- [ ] Push lock-screen: generic text only (xem FCM checklist)
- [ ] In-app notification có thể chi tiết hơn — chỉ user đã login
- [ ] CCCD/CMND: masked trên API; encrypted at rest khi có `PII_ENCRYPTION_KEY`

---

## Security regression testing

- [ ] Login / refresh token flow
- [ ] Booking chỉ truy cập được bởi owner hoặc role phù hợp
- [ ] Guide schedule override chỉ guide/admin
- [ ] Chatbot anonymous rate limit
- [ ] **Verify no raw tokens or GPS appear in logs** (BE + Android logcat)
- [ ] Swagger không expose trên production

---

## Backup and rollback plan

- [ ] `pg_dump` full backup trước deploy (staging test restore trước)
- [ ] DigitalOcean DB backup/snapshot enabled (xác nhận trên dashboard)
- [ ] Rollback app: redeploy image/tag trước
- [ ] Rollback DB: restore từ dump (chỉ với approval)
- [ ] Flyway failed migration playbook

---

## Monitoring and logs

- [ ] `/api/health` monitored (DO health check đã cấu hình)
- [ ] Log aggregation cho `event=push_sent|push_failed|push_invalid_token`
- [ ] Alert khi push FAILED rate cao ( **chưa có trong code** — cần thiết lập ngoài app)
- [ ] DB connection / pool errors
- [ ] OpenRouter API errors (chatbot)

---

## Staging test plan

### Automated

- [ ] **Run backend tests** — `mvn test`
- [ ] **Run Android tests** — `gradlew test`
- [ ] **Run Android assembleDebug**
- [ ] **Run frontend build if available** — `cd Client/FE/website && npm ci && npm run build`

### Manual smoke tests

- [ ] **Smoke test for login** (web + mobile)
- [ ] **Smoke test for booking journey** (tạo → thanh toán sandbox → xác nhận)
- [ ] **Smoke test for Flora chatbot** (auth + rate limit)
- [ ] **Smoke test for nearby recommendations** (consent + radius)
- [ ] **Smoke test for session schedule update** (guide publish override)
- [ ] **Smoke test for in-app notification** (reminder hoặc schedule changed)
- [ ] **Run FCM real-device smoke test** (staging credentials)
- [ ] Post-tour feedback + preference learning (consent on/off)

---

## Production rollout plan

1. **T-7d:** Freeze schema; chuẩn bị backup + staging mirror
2. **T-3d:** FCM staging sign-off trên thiết bị thật
3. **T-1d:** Backup full production (**🔒 approval**)
4. **Deploy window:**
   - Deploy BE image mới (profile `cloud`)
   - Hibernate `update` vẫn active **cho đến** Flyway Phase 1.7B — ghi nhận rủi ro
   - Deploy FE (GitHub Pages) nếu có thay đổi
   - Phát hành APK/AAB Android staging → prod track
5. **T+1h:** Post-release verification (mục dưới)
6. **T+24h:** Theo dõi push failures + DB metrics

---

## Post-release verification

- [ ] `GET https://<api>/api/health` → `UP`
- [ ] Login production
- [ ] Flora chatbot trả lời (có `OPENROUTER_API_KEY`)
- [ ] Tạo booking test nhỏ hoặc đọc booking existing
- [ ] Guide cập nhật schedule → traveler thấy in-app notification
- [ ] FCM (nếu bật): 1 push test controlled
- [ ] Không spike 5xx trong logs

---

## Known risks

| Rủi ro | Mức | Ghi chú |
|--------|-----|---------|
| Hibernate `ddl-auto: update` trên production | **Cao** | DDL không kiểm soát version |
| Không Flyway | **Cao** | Phase 1.7B bắt buộc trước scale |
| Schema prod ≠ entity | **Cao** | Chưa verify trong audit này |
| FCM credentials thiếu | **Cao** | Push tắt; in-app vẫn hoạt động |
| `PII_ENCRYPTION_KEY` thiếu | **Cao** | Token/CCCD plaintext |
| Không staging DB chính thức | **Trung bình** | Test trên copy manual |
| Health check không ping DB | **Trung bình** | App UP nhưng DB down |
| Redis excluded trên cloud | **Thấp** | Rate limit in-memory |
| MoMo sandbox defaults trong yml | **Trung bình** | Phải override env prod |

---

## Go / No-Go decision

### Điều kiện **Go** (tối thiểu)

- Backend + Android tests pass trên CI
- Backup production verified (hoặc DO auto-backup confirmed)
- Smoke staging pass cho login, booking, Flora core
- Secrets production set (`JWT_SECRET`, `DB_PASSWORD` minimum)
- Rollback owner assigned

### Điều kiện **No-Go**

- Bất kỳ BLOCKER nào ở mục Known risks chưa mitigated **và** release thay đổi schema/FCM
- Chưa test FCM trên thiết bị thật khi release bật push
- Chưa backup trước migration/schema deploy

### Trạng thái audit 1.7A (chỉ đọc code)

| Quyết định | Lý do |
|------------|-------|
| **No-Go** cho production FCM + Flyway cutover | Thiếu credentials, staging proof, backup tested |
| **Conditional Go** cho deploy code Flora (in-app only) | In-app path độc lập FCM; vẫn rủi ro `ddl-auto: update` |

**Người quyết định:** _______________  
**Ngày:** _______________

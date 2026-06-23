# Flora AI Phase 1.7A — Kiểm toán Flyway và schema database

**Ngày kiểm toán:** 2026-06-23  
**Phạm vi:** Chỉ đọc codebase và cấu hình; không cài Flyway, không thay `ddl-auto`, không truy cập production DB.  
**Nguồn tham chiếu:** `application.yml`, `application-dev.yml`, `application-cloud.yml`, `pom.xml`, `Dockerfile`, `.do/app.yaml`, toàn bộ entity JPA.

---

## 1. Tóm tắt cấu hình database hiện tại

### 1.1 Spring profiles

| Profile | File | Kích hoạt khi | Database | `ddl-auto` |
|---------|------|---------------|----------|------------|
| *(mặc định)* | `application.yml` | `mvn spring-boot:run` không set profile; JAR local trỏ PostgreSQL | PostgreSQL (`jdbc:postgresql://...`) | **`update`** |
| `dev` | `application-dev.yml` | `SPRING_PROFILES_ACTIVE=dev` hoặc `-Dspring-boot.run.profiles=dev` | H2 in-memory | **`create-drop`** |
| `cloud` | `application-cloud.yml` | Docker `ENV SPRING_PROFILES_ACTIVE=cloud`; DigitalOcean App Platform | PostgreSQL DO Managed (Singapore) | **Kế thừa `update`** từ `application.yml` |

**Không có** profile `staging`, `test`, hay `prod` riêng trong repo.

### 1.2 Môi trường dự kiến

| Môi trường | Cấu hình thực tế trong repo | Ghi chú |
|------------|------------------------------|---------|
| **Local dev (nhanh)** | Profile `dev` → H2, seed chatbot bật | Redis tắt; schema mất sau restart |
| **Local dev (thật)** | Mặc định + `BE/.env` hoặc biến `DB_*` → PostgreSQL local port 5433 | Hibernate tự `update` |
| **Staging** | **Không được định nghĩa** — thường dùng copy DB production hoặc cùng cluster DO với DB riêng | Cần thiết lập thủ công |
| **Production** | Profile `cloud` + DigitalOcean App Platform (`.do/app.yaml`) | PostgreSQL: `db-postgresql-sgp1-flourishtourism-do-user-37760190-0.m.db.ondigitalocean.com:25060/defaultdb` |

### 1.3 Hibernate / JPA

```yaml
# application.yml (PostgreSQL — mặc định và cloud)
spring.jpa.database-platform: org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto: update
spring.jpa.properties.hibernate.default_schema: public
spring.jpa.open-in-view: false
```

```yaml
# application-dev.yml (H2)
spring.jpa.database-platform: org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto: create-drop
```

**Flyway / Liquibase:** Không có dependency trong `pom.xml`.

### 1.4 H2 vs PostgreSQL — khác biệt hành vi

| Khía cạnh | PostgreSQL (prod/local) | H2 (dev profile) |
|-----------|-------------------------|------------------|
| Schema | `public`, tồn tại lâu dài | In-memory, **xóa mỗi lần shutdown** (`create-drop`) |
| `ddl-auto` | `update` — thêm/sửa cột, **không** drop bảng | `create-drop` — tạo lại toàn bộ |
| JSONB | `notifications.data` dùng `columnDefinition = "jsonb"` + `@JdbcTypeCode(SqlTypes.JSON)` | Hibernate map JSON; **không đảm bảo** parity JSONB/constraint với PG |
| UUID PK | `GenerationType.UUID` trên `BaseEntity` | Tương thích test |
| Seed data | `app.seed.enabled=true` (mặc định); `cloud`: **false** | Nhiều seeder chạy (`RoleSeeder`, `DemoDataSeeder`, …) |
| Redis | Bật trong `application.yml` | **Tắt** (exclude autoconfig) |
| `TourCatalogSchemaPatch` | Chạy `ALTER TABLE ... IF NOT EXISTS` + `CREATE TABLE IF NOT EXISTS travel_tickets` lúc startup | Có thể chạy trên H2 với syntax PG — patch bọc try/catch |

### 1.5 Backup / restore hiện có

| Hạng mục | Trạng thái trong repo |
|----------|----------------------|
| Script `pg_dump` / restore | **Không có** |
| Tài liệu backup DO | Chỉ nhắc trong `README.md` / `.github/DEPLOY.md` — không có quy trình kiểm thử |
| Backup tự động DigitalOcean | **Ngoài repo** — phụ thuộc cấu hình cluster DO (chưa xác minh trong code) |
| Khôi phục dữ liệu | Không có playbook |

### 1.6 Khởi tạo database trống

1. **PostgreSQL:** Tạo database `flourishtravel` (hoặc `defaultdb` trên DO) → chạy app với `ddl-auto: update` → Hibernate tạo bảng (`README.md` dòng 174).
2. **Seed:** `app.seed.enabled=true` (local) chạy `RoleSeeder`, `UserSeeder`, `TourSeeder`, `DemoDataSeeder`, … — profile `cloud` **tắt seed**.
3. **`TourCatalogSchemaPatch`:** Bổ sung cột `tours.*` và bảng `travel_tickets` nếu thiếu (song song với Hibernate).
4. **H2 dev:** Không cần DB trống — tự tạo/xóa mỗi session.

### 1.7 Phụ thuộc Hibernate tạo bảng

**Có.** Production (`cloud`) vẫn dùng `ddl-auto: update`. Startup **phụ thuộc** Hibernate (và một phần `TourCatalogSchemaPatch`) để đồng bộ schema khi deploy entity mới. Không có migration versioned.

### 1.8 Chế độ `validate` hiện tại

- **Chưa được cấu hình** trong bất kỳ profile nào.
- **Có thể** chạy thử bằng `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` **sau khi** schema production khớp entity (xem mục 5).
- **Rủi ro:** `TourCatalogSchemaPatch` vẫn chạy `ALTER TABLE` sau startup — mâu thuẫn với triết lý “chỉ validate, không đổi schema” nếu chưa tắt patch.

### 1.9 Biến môi trường nhạy cảm

| Biến | Mục đích | Ghi chú |
|------|----------|---------|
| `DB_PASSWORD` | PostgreSQL | Bắt buộc trên `cloud` |
| `JWT_SECRET` | Ký JWT | Default không an toàn trong `application.yml` |
| `PII_ENCRYPTION_KEY` | AES-256-GCM cho CCCD + FCM token (`PiiEncryptionConverter`) | **Rỗng = lưu plaintext** |
| `GOOGLE_APPLICATION_CREDENTIALS` | Đường dẫn file service account Firebase | Chỉ khi `FCM_ENABLED=true` |
| `FCM_ENABLED` | Bật FCM push | Mặc định `false` |
| `OPENROUTER_API_KEY` | Flora chatbot LLM | |
| `MOMO_*` | Thanh toán sandbox mặc định trong yml | |
| `S3_UPLOAD_*` / `AWS_*` | Upload ảnh S3 | |
| `GOOGLE_MAPS_API_KEY` | Maps | |
| `REDIS_PASSWORD` | Redis | |

### 1.10 File an toàn commit vs không bao giờ commit

| An toàn commit | Không bao giờ commit |
|----------------|----------------------|
| `application.yml`, `application-*.yml` (không chứa secret thật) | `.env`, `env`, `*.env` |
| `.env.example` | `application-local.yml` |
| `Dockerfile`, `.do/app.yaml` (host DB public — đã có trong repo) | Firebase `google-services.json`, service account JSON |
| `google-services.json.example` (Mobile) | `BE/env` (file không dấu chấm — **chưa** trong `.gitignore`) |
| Entity, docs | Private keys, `PII_ENCRYPTION_KEY` production |

---

## 2. Kiểm kê schema (52 bảng JPA)

Tất cả entity kế thừa `BaseEntity`: PK `id UUID`, `created_at`, `updated_at`.

### 2.1 Bảng tổng hợp

| Entity / Table | Mới / Có sẵn | Cột quan trọng | Ràng buộc / index | Độ nhạy cảm dữ liệu | Mối quan tâm Flyway |
|--------------|--------------|----------------|-------------------|---------------------|---------------------|
| `users` | Có sẵn | email, password_hash, full_name, phone, PII profile | UNIQUE email, employee_code | Cao (PII, auth) | Baseline |
| `roles` | Có sẵn | name | — | Thấp | Seed roles |
| `user_providers` | Có sẵn | provider, provider_user_id | UNIQUE (provider, provider_user_id) | Trung bình | Baseline |
| `user_favorites` | Có sẵn | user_id, tour_id | Index user | Thấp | Baseline |
| `refresh_tokens` | Có sẵn | token, user_id | — | Cao | Baseline |
| `password_reset_tokens` | Có sẵn | token_hash | UNIQUE token_hash | Cao | Baseline |
| **`user_travel_preferences`** | **Flora mới** | consent flags, travel_styles, food_allergies | UNIQUE user_id | Cao (sở thích, consent) | Migration mới nếu prod chưa có |
| `bookings` | Có sẵn | user_id, session_id, status, pickup_address | FK user, session; index status | Cao | FK ownership |
| `booking_guests` | Có sẵn | id_number (**encrypted**), full_name | FK booking_id | **Rất cao** | Encrypted column |
| `session_participants` | Có sẵn | roster_key, line_index | UNIQUE (session, booking, roster_key); UNIQUE (session, booking, line_index) | Trung bình | Baseline |
| `session_participant_activity_attendance` | Có sẵn | check-in times | UNIQUE (participant, activity) | Trung bình | Baseline |
| `session_checkins` | Có sẵn | — | — | Trung bình | Baseline |
| `session_waitlist` | Có sẵn | — | — | Thấp | Baseline |
| `promotions` | Có sẵn | code | UNIQUE code | Thấp | Baseline |
| `payments` | Có sẵn | order_id, status | Index booking | Cao | Baseline |
| `refunds` | Có sẵn | — | Index booking | Cao | Baseline |
| `tours` | Có sẵn | slug, badge, destination_city | UNIQUE slug | Thấp | Patch cột qua `TourCatalogSchemaPatch` |
| `categories` | Có sẵn | slug | UNIQUE slug | Thấp | Baseline |
| `tour_sessions` | Có sẵn | start_date, tour_guide_id | Index tour, guide, date | Thấp | Baseline |
| `tour_itineraries` | Có sẵn | day_number | Index tour_id | Thấp | Baseline |
| `tour_activities` | Có sẵn (+ Flora cols) | is_gathering_event, gathering_event_type, schedule_status, location_address | Index itinerary_id | Trung bình (địa điểm) | Cột Flora thêm bởi Hibernate |
| **`tour_session_activity_overrides`** | **Flora mới** | overrides, publication_status, version | **UNIQUE (tour_session_id, tour_activity_id)**; index publication_status | Trung bình (lịch, địa điểm) | **Bảng mới — flag** |
| `tour_locations` | Có sẵn | lat/lng | Index tour_id | Trung bình | Baseline |
| `tour_images` / `tour_videos` | Có sẵn | — | Index tour_id | Thấp | Baseline |
| `reviews` | Có sẵn (+ Flora) | rating, comment, **feedback_tags** | UNIQUE booking_id | Trung bình | **feedback_tags** TEXT mới |
| `notifications` | Có sẵn | type, title, body, **data JSONB** | Index user, is_read, created_at | Trung bình | **JSONB** PG-only type |
| **`flora_reminder_deliveries`** | **Flora mới** | reminder_type, idempotency_key, status | **UNIQUE idempotency_key**; index booking, user | Trung bình | **Idempotency key — flag** |
| **`push_devices`** | **FCM mới** | **token_ciphertext** (encrypted), **token_hash** | **UNIQUE token_hash**; index user, active | **Rất cao** (FCM token) | **Encrypted + hash — flag** |
| **`push_notification_deliveries`** | **FCM mới** | status, attempt_count, next_attempt_at | **UNIQUE (notification_id, push_device_id)** | Trung bình | **Idempotency delivery — flag** |
| **`user_location_pings`** | **Flora mới** | latitude, longitude, captured_at | Index booking, user, captured_at | **Rất cao** (GPS) | Retention policy |
| `search_logs` | Chatbot | search_query, session_id | Index user, created_at | Trung bình | Baseline |
| `chatbot_intent` / `chatbot_intent_training_phrase` / `chatbot_training_phrase` / `chatbot_global_config` / `policy_faq` | Chatbot | — | UNIQUE keys | Thấp | Seed chatbot |
| `chat_rooms` | Có sẵn | session_id | UNIQUE session_id | Thấp | Baseline |
| `chat_members` | Có sẵn | — | UNIQUE (room, user) | Thấp | Baseline |
| `messages` | Có sẵn | content | Index room, created_at | Trung bình | Baseline |
| `message_reactions` | Có sẵn | — | UNIQUE (message, user, type) | Thấp | Baseline |
| `destinations` + bảng con (attractions, reviews, map_pois, …) | Có sẵn | — | — | Thấp | Baseline |
| `contact_requests` | Có sẵn | email | — | Trung bình | Baseline |
| `trip_plans` | Có sẵn | — | — | Thấp | Baseline |
| `travel_tickets` | Có sẵn / patch | slug | UNIQUE slug; có thể tạo bởi `TourCatalogSchemaPatch` | Thấp | Trùng Hibernate vs patch |
| `audit_logs` | Có sẵn | — | — | Trung bình | Baseline |

**Tổng:** **52 entity / bảng** cần rà soát khi baseline Flyway (gồm **7 bảng/cột Flora/FCM mới** đáng chú ý).

### 2.2 Cờ đặc biệt (bắt buộc kiểm tra trên production)

| Cờ | Chi tiết từ code |
|----|------------------|
| **UUID columns** | Mọi bảng: `id UUID` qua `BaseEntity`; FK dạng UUID |
| **JSON / JSONB** | `notifications.data` — `jsonb` PostgreSQL only |
| **Encrypted FCM token** | `push_devices.token_ciphertext` — `PiiEncryptionConverter`; plaintext nếu thiếu `PII_ENCRYPTION_KEY` |
| **token_hash unique** | `push_devices.token_hash` VARCHAR(64) UNIQUE |
| **feedback_tags** | `reviews.feedback_tags` TEXT (CSV tag IDs) |
| **session override unique** | `tour_session_activity_overrides`: UNIQUE `(tour_session_id, tour_activity_id)` |
| **notification data JSON** | In-app lưu `{"bookingId":"..."}` — **không** đưa vào FCM payload visible |
| **booking ownership FK** | `bookings.user_id`, `flora_reminder_deliveries.booking_id`, `user_location_pings.booking_id` |
| **Flora reminder idempotency** | `flora_reminder_deliveries.idempotency_key` UNIQUE (max 200 chars) |
| **push delivery idempotency** | `push_notification_deliveries`: UNIQUE `(notification_id, push_device_id)` |

---

## 3. Kế hoạch áp dụng Flyway (chưa triển khai)

### 3.1 Kịch bản A — PostgreSQL production đã có dữ liệu (Hibernate đã tạo bảng)

**Mục tiêu:** Chuyển sang Flyway **không** drop/recreate, **không** mất dữ liệu khách hàng.

#### Có nên dùng `baselineOnMigrate`?

**Có — phù hợp.** Production đã có schema do Hibernate `update`. Flyway cần:
- `flyway.baseline-on-migrate=true`
- `flyway.baseline-version=1` (hoặc `0` tùy convention team)
- Migration đầu tiên **không** chạy `CREATE TABLE` cho bảng đã tồn tại — chỉ `baseline` metadata.

#### Chiến lược baseline version

1. **`V1__baseline.sql`** — file **trống** hoặc chỉ comment: “Schema đã tồn tại trước Flyway, tạo bởi Hibernate đến commit `<hash>`”.
2. Đánh dấu baseline tại version `1` trên DB production **sau** khi xác nhận schema snapshot khớp entity.
3. Migration thực tế bắt đầu từ **`V2__...`** trở đi (chỉ thay đổi incremental).

#### Snapshot schema trước baseline

1. Chạy backup schema-only (mục 4) trên **bản sao staging** của production.
2. Export `\d+` / `information_schema` cho 52 bảng.
3. So sánh với entity JPA + diff Hibernate `validate` (mục 4).

#### Xác nhận schema deployed khớp entity

1. Copy DB production → staging (DO snapshot hoặc `pg_dump`).
2. Chạy app với `ddl-auto=validate` **chỉ trên staging** — ghi nhận lỗi thiếu cột/bảng.
3. Đối chiếu thủ công 7 artifact Flora/FCM (mục 2.2).
4. Lưu artifact diff vào ticket release.

#### Đánh số migration tương lai

- Format: `V{major}__{mô_tả_snake}.sql` — ví dụ `V2__add_push_devices.sql` **chỉ nếu** bảng chưa có trên prod cũ.
- Một thay đổi logic = một file; không gộp unrelated DDL.
- Sau baseline, **cấm** Hibernate `update` trên production.

#### Ngăn Flyway chạy nhầm database

- Biến `FLYWAY_URL` / `spring.flyway.url` **riêng**, không dùng default localhost.
- CI/CD: gate theo `DB_HOST` whitelist (hostname DO cụ thể).
- Profile `dev` (H2): **tắt Flyway** hoặc `locations=classpath:db/migration/h2` tách biệt.
- Bắt buộc `spring.flyway.enabled=false` cho đến khi Phase 1.7B hoàn tất test.

#### Chuyển production từ `update` → `validate`

**Thứ tự an toàn:**
1. Backup full production (mục 4) — **cổng phê duyệt thủ công**.
2. Baseline Flyway trên staging đã verify.
3. Deploy BE với Flyway enabled + `ddl-auto=validate` trên staging → smoke test.
4. Production: baseline → deploy `validate` → **không** rollback `ddl-auto` về `update`.
5. **Tắt hoặc chuyển** `TourCatalogSchemaPatch` sang migration Flyway tương ứng (hiện patch chạy `ALTER` ngoài Hibernate).

#### Rollback khi migration fail

- Flyway **không** auto-rollback DDL — chuẩn bị script `U{n}__rollback_*.sql` thủ công cho migration rủi ro cao.
- Nếu fail giữa deploy: giữ container cũ; restore DB từ backup nếu DDL đã chạy một phần (transaction per migration).
- Ghi `flyway_schema_history` trước/sau mỗi deploy.

#### Test trên staging DB copy

1. `pg_dump` production → restore staging cluster.
2. Chạy baseline + migrations.
3. `mvn test` + smoke test Flora/FCM.
4. Chỉ sau pass → xin approval production.

#### Cổng phê duyệt thủ công trước production

- [ ] Backup full verified (checksum / restore test)
- [ ] Schema diff signed off
- [ ] Staging flyway + validate pass 24h
- [ ] Maintenance window (nếu DDL lock bảng lớn)
- [ ] Rollback owner on-call

---

### 3.2 Kịch bản B — Database PostgreSQL trống hoàn toàn

#### Tạo full schema

**Hai lựa chọn (Phase 1.7B):**

1. **Khuyến nghị:** Migration `V1__initial_schema.sql` generated từ `pg_dump --schema-only` của staging đã validate — **một file baseline đầy đủ** 52 bảng + index + FK.
2. **Tạm thời (không khuyến nghị prod):** Chạy app `ddl-auto=update` một lần → `pg_dump --schema-only` → chuyển thành Flyway V1 → chuyển sang `validate`.

#### Baseline có cần tất cả bảng?

**Có** — môi trường mới không nên phụ thuộc Hibernate tạo bảng. Một baseline đầy đủ đảm bảo reproducibility.

#### PostgreSQL-only types

- `UUID` → `gen_random_uuid()` hoặc app-generated UUID.
- `jsonb` → `notifications.data` — dùng `JSONB` explicit trong SQL migration.
- `TIMESTAMPTZ` cho `Instant` auditing fields.

#### H2 dev/testing khác PostgreSQL

- CI hiện chạy `mvn test` — có thể dùng H2 với `create-drop` **không qua Flyway**.
- **Không** dùng cùng file migration PG cho H2 nếu có `jsonb`/syntax PG.
- Tách: `db/migration/postgresql/` vs test profile H2 in-memory.

#### Seed sau schema

1. `RoleSeeder` — roles bắt buộc cho auth.
2. `ChatbotConfigSeeder` — nếu `SEED_CHATBOT_CONFIG=true`.
3. `DemoDataSeeder` — **chỉ** dev/staging, **không** production (`app.seed.enabled=false` trên cloud).

#### Verify môi trường mới

1. Flyway migrate → `validate` startup.
2. `GET /api/health` → `UP`.
3. Login smoke + tạo booking test.
4. `mvn test` trên CI.

---

## 4. Lệnh an toàn (ví dụ — **không thực thi tự động**)

> Tất cả lệnh dùng placeholder. **Chạy trên staging trước.** Mục đánh dấu **🔒 Cần phê duyệt production thủ công**.

### 4.1 Backup schema-only PostgreSQL

```bash
# Chạy trên staging trước
pg_dump "${DATABASE_URL}" \
  --schema-only \
  --no-owner \
  --no-privileges \
  -f flourish_schema_$(date +%Y%m%d).sql
```

`DATABASE_URL` dạng: `postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=${DB_SSL_MODE}`

### 4.2 Backup full PostgreSQL

```bash
# Chạy trên staging trước
# 🔒 Cần phê duyệt production thủ công trước khi chạy trên prod
pg_dump "${DATABASE_URL}" \
  --format=custom \
  --no-owner \
  -f flourish_full_$(date +%Y%m%d).dump
```

### 4.3 Xác minh restore (staging)

```bash
# Chạy trên staging trước — restore vào DB staging mới, không ghi đè prod
pg_restore --dbname="${STAGING_DATABASE_URL}" --verbose flourish_full_YYYYMMDD.dump
# Sau restore: đếm bảng, chạy smoke test read-only
psql "${STAGING_DATABASE_URL}" -c "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';"
```

### 4.4 Truy vấn read-only — danh sách bảng

```sql
-- Chạy trên staging trước
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

### 4.5 Metadata cột

```sql
-- Chạy trên staging trước
SELECT table_name, column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
ORDER BY table_name, ordinal_position;
```

### 4.6 Index và constraint

```sql
-- Chạy trên staging trước
SELECT tc.table_name, tc.constraint_name, tc.constraint_type, kcu.column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
  ON tc.constraint_name = kcu.constraint_name
WHERE tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_type;

SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;
```

### 4.7 Kiểm tra lịch sử Flyway (sau Phase 1.7B)

```sql
-- Chạy trên staging trước
SELECT installed_rank, version, description, type, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

### 4.8 Chạy app ở chế độ validate schema

```bash
# Chạy trên staging trước — không dùng profile dev (H2)
export SPRING_PROFILES_ACTIVE=cloud
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate
# 🔒 Production: chỉ sau backup + approval
java -jar flourish-travel-backend-1.0.0-SNAPSHOT.jar
```

---

## 5. Triển khai và CI/CD (tham chiếu)

| Thành phần | File | Ghi chú |
|------------|------|---------|
| Docker image | `Server/BE/Dockerfile` | `SPRING_PROFILES_ACTIVE=cloud` mặc định |
| DigitalOcean | `.do/app.yaml` | Health `/api/health`, delay 300s; secrets: `DB_PASSWORD`, `JWT_SECRET` |
| GitHub Actions BE | `.github/workflows/ci.yml` | `mvn package` + `mvn test` — **không** deploy DB |
| Health | `HealthController` `/health` | Không kiểm DB connectivity |
| OpenAPI | `springdoc` | **Tắt** trên profile `cloud` |
| Actuator | `management.endpoints` | Chỉ `health` trên cloud |

---

## 6. Phân loại phát hiện (Flyway / DB)

| Mức | Phát hiện |
|-----|-----------|
| **BLOCKER** | Không có Flyway; production dùng `ddl-auto: update` — không kiểm soát DDL |
| **BLOCKER** | Không có script/quy trình backup **đã kiểm thử** trong repo |
| **HIGH RISK** | Schema production vs entity **chưa xác minh** (không truy cập DB trong audit này) |
| **HIGH RISK** | `TourCatalogSchemaPatch` chạy DDL ngoài Hibernate — khó chuyển `validate` |
| **HIGH RISK** | Không có môi trường staging được định nghĩa trong cấu hình |
| **MEDIUM RISK** | H2 `create-drop` không phản ánh PG JSONB/constraints |
| **MEDIUM RISK** | File `env` (không dấu chấm) có thể chứa credential — không trong `.gitignore` |
| **LOW RISK** | Health check không validate DB connection |
| **READY** | 52 entity được map rõ; tài liệu Flora ghi nhận các cột mới |
| **READY** | Kế hoạch baseline + `baselineOnMigrate` **khả thi** nếu có snapshot staging |

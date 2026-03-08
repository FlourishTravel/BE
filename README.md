# FlourishTravel Backend (Spring Boot)

Backend API cho dự án FlourishTravel: quản lý tour, đặt chỗ, thanh toán MoMo, chatbot AI, chat nhóm real-time.

## Công nghệ

- **Java 17**, **Spring Boot 3.2**
- **PostgreSQL**, **Redis**
- **Spring Security**, **JWT** (access + refresh token)
- **Spring WebSocket** (chat)
- **Spring Data JPA**

## Cấu trúc thư mục chi tiết

```
BE/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/flourishtravel/
    │   │   ├── FlourishTravelApplication.java
    │   │   │
    │   │   ├── config/                      # Cấu hình
    │   │   │   ├── SecurityConfig.java      # Spring Security, JWT filter
    │   │   │   ├── CorsConfig.java          # CORS
    │   │   │   ├── WebSocketConfig.java     # WebSocket /ws/chat
    │   │   │   └── PasswordEncoderConfig.java
    │   │   │
    │   │   ├── common/                      # Dùng chung
    │   │   │   ├── entity/
    │   │   │   │   └── BaseEntity.java       # id, createdAt, updatedAt
    │   │   │   ├── dto/
    │   │   │   │   └── ApiResponse.java     # Chuẩn response API
    │   │   │   └── exception/
    │   │   │       ├── ResourceNotFoundException.java
    │   │   │       ├── BadRequestException.java
    │   │   │       └── GlobalExceptionHandler.java
    │   │   │
    │   │   ├── security/                    # JWT & UserDetails
    │   │   │   ├── JwtProperties.java
    │   │   │   ├── JwtProvider.java         # Tạo/parse JWT
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   ├── JwtEntryPoint.java
    │   │   │   └── UserPrincipal.java       # UserDetails từ User
    │   │   │
    │   │   ├── chat/                        # WebSocket chat
    │   │   │   └── websocket/
    │   │   │       └── ChatWebSocketHandler.java
    │   │   │
    │   │   └── domain/
    │   │       ├── user/                    # Người dùng & RBAC
    │   │       │   ├── entity/
    │   │       │   │   ├── User.java
    │   │       │   │   ├── Role.java
    │   │       │   │   ├── UserProvider.java # OAuth
    │   │       │   │   └── RefreshToken.java
    │   │       │   ├── repository/
    │   │       │   │   ├── UserRepository.java
    │   │       │   │   ├── RoleRepository.java
    │   │       │   │   ├── UserProviderRepository.java
    │   │       │   │   └── RefreshTokenRepository.java
    │   │       │   └── service/
    │   │       │       └── CustomUserDetailsService.java
    │   │       │
    │   │       ├── auth/                    # Đăng ký, đăng nhập, refresh
    │   │       │   ├── dto/
    │   │       │   │   ├── LoginRequest.java
    │   │       │   │   ├── RegisterRequest.java
    │   │       │   │   └── AuthResponse.java
    │   │       │   ├── service/
    │   │       │   │   └── AuthService.java
    │   │       │   └── controller/
    │   │       │       └── AuthController.java  # POST /auth/register, login, refresh, logout
    │   │       │
    │   │       ├── tour/                    # Tour, Session, Category, Location, Image, Video
    │   │       │   ├── entity/
    │   │       │   │   ├── Category.java
    │   │       │   │   ├── Tour.java
    │   │       │   │   ├── TourSession.java
    │   │       │   │   ├── TourLocation.java
    │   │       │   │   ├── TourItinerary.java
    │   │       │   │   ├── TourImage.java
    │   │       │   │   └── TourVideo.java
    │   │       │   ├── repository/
    │   │       │   │   ├── TourRepository.java
    │   │       │   │   ├── TourSessionRepository.java
    │   │       │   │   └── CategoryRepository.java
    │   │       │   ├── service/
    │   │       │   │   └── TourService.java
    │   │       │   └── controller/
    │   │       │       └── TourController.java   # GET /tours, /tours/:id, /tours/by-slug/:slug
    │   │       │
    │   │       ├── booking/                 # Đặt chỗ, Mã khuyến mãi
    │   │       │   ├── entity/
    │   │       │   │   ├── Booking.java
    │   │       │   │   └── Promotion.java
    │   │       │   └── repository/
    │   │       │       └── BookingRepository.java
    │   │       │
    │   │       ├── payment/                 # Thanh toán MoMo, Hoàn tiền
    │   │       │   ├── entity/
    │   │       │   │   ├── Payment.java
    │   │       │   │   └── Refund.java
    │   │       │   └── controller/
    │   │       │       └── MomoWebhookController.java  # POST /payments/momo/ipn
    │   │       │
    │   │       ├── chat/                    # Phòng chat, Tin nhắn, Ghim, Reaction
    │   │       │   └── entity/
    │   │       │       ├── ChatRoom.java
    │   │       │       ├── ChatMember.java
    │   │       │       ├── Message.java
    │   │       │       └── MessageReaction.java
    │   │       │
    │   │       └── chatbot/                 # Trợ lý AI
    │   │           ├── dto/
    │   │           │   ├── ChatbotRequest.java
    │   │           │   └── ChatbotResponse.java
    │   │           ├── service/
    │   │           │   └── ChatbotService.java
    │   │           └── controller/
    │   │               └── ChatbotController.java  # POST /chatbot/message
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── application-dev.yml
    │
    └── test/
        └── java/com/flourishtravel/
            └── (tests)
```

## Chạy dự án

### Chạy nhanh (profile `dev` – không cần PostgreSQL/Redis)

**Nếu gặp lỗi "Connection to localhost:5432 refused"** → bạn đang chạy không có profile; cần chạy với profile `dev` (xem bên dưới) hoặc bật PostgreSQL.

Profile **dev** dùng **H2 in-memory** và tắt Redis, chạy được ngay không cần cài DB:

- **Windows PowerShell:**
  ```powershell
  cd BE
  mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
  ```
- **Linux/macOS hoặc CMD:**
  ```bash
  cd BE
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  ```

Sau khi chạy:
- API: **http://localhost:8080/api**
- Swagger: **http://localhost:8080/api/swagger-ui.html**
- H2 Console: **http://localhost:8080/api/h2-console** (JDBC URL: `jdbc:h2:mem:flourishtravel`, user: `sa`, password để trống)

### Chạy với PostgreSQL (pgAdmin)

1. **Tạo database một lần trong pgAdmin**
   - Mở pgAdmin 4 → kết nối server PostgreSQL.
   - Chuột phải **Databases** → **Create** → **Database**.
   - **Database**: `flourishtravel` (hoặc tên trong `DB_NAME`).
   - Save.

2. **Chạy ứng dụng** (không bật profile `dev`):
   - **PowerShell:** `mvn spring-boot:run`
   - **CMD/Bash:** `mvn spring-boot:run`
   - Đảm bảo `application.yml` (hoặc biến môi trường) có `DB_NAME=flourishtravel`, `DB_USER`, `DB_PASSWORD` đúng với PostgreSQL.

3. **Hibernate sẽ tự tạo toàn bộ bảng** trong database đó (`ddl-auto: update`). Không cần tạo bảng tay trong pgAdmin.

### Yêu cầu

- JDK 17+
- Maven 3.8+
- PostgreSQL (chạy local hoặc Docker) – *không cần nếu dùng profile `dev`*
- Redis (tùy chọn, cho cache/chat) – *tắt khi dùng profile `dev`*

### Biến môi trường (.env hoặc export)

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=flourishtravel
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-256-bit-secret-key-at-least-32-characters-long
REDIS_HOST=localhost
REDIS_PORT=6379
MOMO_PARTNER_CODE=
MOMO_ACCESS_KEY=
MOMO_SECRET_KEY=
MOMO_IPN_URL=http://localhost:8080/api/payments/momo/ipn
FRONTEND_URL=http://localhost:5173

# Chatbot AI (chỉ cần 1 trong 2; ưu tiên Gemini nếu set cả hai)
GEMINI_API_KEY=      # Lấy tại: https://aistudio.google.com/apikey
OPENAI_API_KEY=      # Lấy tại: https://platform.openai.com/api-keys
# Tùy chọn: GEMINI_MODEL=gemini-1.5-flash, OPENAI_MODEL=gpt-4o-mini
PII_ENCRYPTION_KEY=   # base64 32 bytes – mã hóa CCCD/CMND trong DB (tùy chọn, production nên set)
```

**Bảo mật CCCD/CMND:** Số CCCD/CMND của khách được:
- **Mã hóa khi lưu DB** (AES-256-GCM) nếu set `PII_ENCRYPTION_KEY` (sinh key: `openssl rand -base64 32`).
- **Che khi trả API**: response chỉ có `maskedIdNumber` (vd: `***2345`), không trả full số.

### Lệnh

```bash
cd BE
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

API base: **http://localhost:8080/api**

### Đăng nhập khách hàng (ưu tiên)

| Phương thức | API | Ghi chú |
|-------------|-----|--------|
| **Email + mật khẩu** | `POST /auth/register` (email, password, fullName, phone?) → `POST /auth/login` (email, password) | Quên mật khẩu: `POST /auth/forgot-password` → `POST /auth/reset-password` (token, new_password). Đổi MK (đã đăng nhập): `POST /auth/change-password`. |
| **Google** | `POST /auth/google` body: `{ "id_token": "<id_token từ Google Sign-In>" }` | FE: Google Sign-In → lấy credential.id_token → gửi lên. |
| **Facebook** | `POST /auth/facebook` body: `{ "id_token": "<id_token từ Facebook Login>" }` | FE: Facebook Login SDK → lấy id_token → gửi lên. |

Hoặc dùng chung: `POST /auth/oauth` với `{ "provider": "google"|"facebook", "id_token": "..." }`.

Cả ba đều trả `{ data: { accessToken, refreshToken, expiresIn, user: { id, email, fullName, role, avatarUrl } } }`. FE lưu token, gửi header `Authorization: Bearer <accessToken>` cho các API cần đăng nhập.

**Swagger UI (test API):** **http://localhost:8080/api/swagger-ui.html**  
- Mở trình duyệt, vào link trên để xem và gọi thử các endpoint.
- Các API cần đăng nhập: bấm **Authorize**, nhập `Bearer <access_token>` (token lấy từ `POST /auth/login` hoặc `POST /auth/oauth`).

- `POST /api/auth/register` – Đăng ký
- `POST /api/auth/login` – Đăng nhập
- `POST /api/auth/google` – Đăng nhập Google (body: id_token)
- `POST /api/auth/facebook` – Đăng nhập Facebook (body: id_token)
- `POST /api/auth/oauth` – Đăng nhập OAuth chung (provider + id_token)
- `POST /api/auth/refresh` – Đổi access token
- `POST /api/auth/logout` – Đăng xuất
- `GET /api/tours` – Danh sách tour (query: destination, minPrice, maxPrice, startDate)
- `GET /api/tours/:id` – Chi tiết tour
- `GET /api/tours/by-slug/:slug` – Chi tiết tour theo slug
- `POST /api/chatbot/message` – Gửi tin cho trợ lý AI
- `POST /api/payments/momo/ipn` – Webhook MoMo (public)

WebSocket: **ws://localhost:8080/api/ws/chat**

## Cấu hình LLM (Chatbot thông minh)

Trợ lý AI dùng **Gemini** (ưu tiên) hoặc **OpenAI** để phân tích ý định và trả lời tự nhiên. Nếu không cấu hình key, chatbot vẫn chạy ở chế độ fallback (tìm tour theo từ khóa, câu trả lời cố định).

### Load biến từ file `.env` (BE/.env)

Đã có sẵn file **BE/.env** (và **BE/.env.example**). Spring Boot không tự đọc `.env`, nên cần load thủ công khi chạy từ terminal:

- **PowerShell** (trong thư mục `BE`):
  ```powershell
  Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
      [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
    }
  }
  mvn spring-boot:run
  ```
- **Cách khác:** Trong IDE (IntelliJ / VS Code), cấu hình Run → **Env file** trỏ tới `BE/.env`.

File `.env` đã được thêm vào **BE/.gitignore** để không bị commit key lên Git.

### Cách 1: Biến môi trường (khuyến nghị)

**Gemini (Google):**
1. Vào [Google AI Studio](https://aistudio.google.com/apikey) → **Create API key**.
2. Set trước khi chạy:
   - **Windows PowerShell:** `$env:GEMINI_API_KEY="your-api-key-here"`
   - **Linux/macOS:** `export GEMINI_API_KEY=your-api-key-here`
   - Hoặc tạo file `.env` (nếu dùng tool load env) với dòng: `GEMINI_API_KEY=your-api-key-here`

**OpenAI:**
1. Vào [OpenAI API Keys](https://platform.openai.com/api-keys) → tạo key.
2. Set: `OPENAI_API_KEY=sk-...`

Chỉ cần **một trong hai** key. Nếu set cả hai, backend dùng **Gemini** trước.

### Cách 2: application.yml (chỉ dùng khi dev, không commit key)

Trong `BE/src/main/resources/application.yml`, sửa (hoặc override bằng profile):

```yaml
app:
  gemini:
    api-key: ${GEMINI_API_KEY:your-key-here}   # hoặc bỏ :your-key-here, set qua env
    model: gemini-1.5-flash
  openai:
    api-key: ${OPENAI_API_KEY:}
    model: gpt-4o-mini
```

**Lưu ý:** Không commit API key vào Git. Dùng biến môi trường hoặc secret (CI/production).

### Kiểm tra

Sau khi set key và restart backend, gửi tin nhắn trong [demo chatbot](http://localhost:8080/api/chatbot-demo.html) (vd: "Tour biển 3 ngày"). Nếu LLM hoạt động, bot sẽ trả lời có intent/slots và câu tự nhiên thay vì câu fallback cố định.

## Tài liệu liên quan

- Luồng & API: **docs/FLOWS.md**
- ERD bổ sung: **docs/ERD-SUPPLEMENT.md**
- Chatbot: **docs/CHATBOT-FLOW.md**

## Việc còn lại (TODO)

- [ ] Migration Flyway/Liquibase từ ERD (thay `ddl-auto: update` khi production)
- [ ] BookingService, BookingController (tạo đơn, MoMo redirect)
- [ ] Hoàn thiện Momo IPN: verify signature, cập nhật Booking, ChatMember, Notification
- [ ] Chat: MessageRepository, ChatRoomService, join_room/send_message trong WebSocket
- [ ] Guide: SessionCheckin, Schedule API
- [ ] Admin: CRUD Tour/Session/User, Stats, Refund, ContactRequest
- [ ] Tích hợp LLM (OpenAI/Gemini) trong ChatbotService
- [ ] OAuth Google/Facebook
- [ ] Notifications, Favorites, Reviews, ContactRequests, Waitlist (entity + API)

# Flora AI — Phase 1.1 Security Hardening

Tài liệu mô tả các biện pháp bảo mật đã triển khai cho chatbot Flora (POST `/api/chatbot/message` và các endpoint dữ liệu phụ trợ).

## Tổng quan

| Hạng mục | Trạng thái |
|----------|------------|
| Rate limiting (auth / guest) | ✅ In-memory, per-instance |
| Booking context authorization | ✅ JWT-only identity |
| Location validation & consent | ✅ |
| LLM prompt sanitization | ✅ `sanitizeForLlm` |
| Safe structured logging | ✅ |
| Anonymous chatbot | ✅ Vẫn bật |

## Rate limit strategy

### Endpoint được giới hạn

- `POST /api/chatbot/message`
- `GET /api/chatbot/nearby-places`
- `GET /api/chatbot/weather-forecast`

### Khóa client

| Loại | Khóa Redis-style (in-memory) |
|------|------------------------------|
| Đã đăng nhập | `chatbot:user:{userId}` |
| Khách (guest) | `chatbot:anon:{normalizedIp}` |

`userId` lấy từ JWT (`UserPrincipal`) sau `JwtAuthenticationFilter`, **không** từ body request.

### Giới hạn mặc định

| Tier | / phút | / giờ |
|------|--------|-------|
| Authenticated | 30 | 300 |
| Anonymous | 10 | 80 |

Cửa sổ **fixed-window** (phút + giờ). Khi vượt ngưỡng:

- HTTP `429 Too Many Requests`
- Body `ApiResponse` với message tiếng Việt thân thiện
- Header `Retry-After` (giây đến khi cửa sổ reset)

### Lưu trữ hiện tại: in-memory

**Lý do:** Redis có trong `pom.xml` và `application.yml`, nhưng `RedisAutoConfiguration` bị **loại trừ** trong profile `dev` và `cloud`. Không có `RedisTemplate` đang dùng trong mã nguồn — bật Redis-backed limiter ngay có thể làm fail startup trên ECS.

Implementation: `InMemoryChatbotRateLimitStore` với:

- Giới hạn số key (`max-keys`, mặc định 10.000)
- Eviction theo `lastAccess` khi đầy
- `@Scheduled` cleanup entry hết hạn

**Hạn chế production:** Mỗi instance JVM có bộ đếm riêng — client có thể gửi N× limit nếu có nhiều replica. Cần chuyển sang Redis (hoặc API gateway) khi scale ngang.

### Chuyển sang Redis sau này

1. Bật `RedisAutoConfiguration` trên môi trường production (bỏ exclude hoặc profile riêng).
2. Implement `RedisChatbotRateLimitStore implements ChatbotRateLimitStore` dùng `INCR` + `EXPIRE` hoặc sliding window Lua.
3. Đăng ký bean `@Primary` hoặc `@ConditionalOnProperty` thay `InMemoryChatbotRateLimitStore`.
4. Giữ nguyên `ChatbotRateLimitService`, `ChatbotClientKeyResolver`, và filter.

Interface: `com.flourishtravel.domain.chatbot.security.ChatbotRateLimitStore`.

## Cấu hình (`application.yml`)

```yaml
app:
  chatbot:
    rate-limit:
      authenticated:
        requests-per-minute: 30
        requests-per-hour: 300
      anonymous:
        requests-per-minute: 10
        requests-per-hour: 80
      in-memory:
        max-keys: 10000
        cleanup-interval-ms: 300000
      trusted-proxy:
        enabled: false
```

### Biến môi trường

| Biến | Mô tả | Mặc định |
|------|--------|----------|
| `CHATBOT_RATE_LIMIT_AUTH_PER_MINUTE` | Limit/phút (đăng nhập) | 30 |
| `CHATBOT_RATE_LIMIT_AUTH_PER_HOUR` | Limit/giờ (đăng nhập) | 300 |
| `CHATBOT_RATE_LIMIT_ANON_PER_MINUTE` | Limit/phút (guest) | 10 |
| `CHATBOT_RATE_LIMIT_ANON_PER_HOUR` | Limit/giờ (guest) | 80 |
| `CHATBOT_RATE_LIMIT_MAX_KEYS` | Số key tối đa in-memory | 10000 |
| `CHATBOT_RATE_LIMIT_CLEANUP_MS` | Chu kỳ dọn entry | 300000 |
| `CHATBOT_TRUSTED_PROXY` | Tin `X-Forwarded-For` / `X-Real-IP` | false |

Chỉ set `CHATBOT_TRUSTED_PROXY=true` khi reverse proxy (ALB, nginx, Cloudflare) **ghi đè** header và client không gửi trực tiếp tới app. Nếu false, dùng `HttpServletRequest.getRemoteAddr()`.

## Anonymous vs authenticated

| Hành vi | Guest | Đăng nhập |
|---------|-------|-----------|
| Gọi chatbot | ✅ | ✅ |
| Rate limit key | IP | User ID |
| `bookingId` trong body | Trả message đăng nhập, không load journey | Kiểm tra ownership |
| Cá nhân hóa LLM | Không | Cần `personalizationConsent` |
| GPS trong hint | Chỉ gợi ý tức thời (đã validate) | Cần `locationConsent` |
| Body `userId` | **Bỏ qua** | **Bỏ qua** (chỉ JWT) |

## Booking access rules

1. Guest + `bookingId` → response: *"Bạn hãy đăng nhập để Flora có thể hỗ trợ theo thông tin chuyến đi của bạn nhé."*
2. Auth + `bookingId` không thuộc user → strip `bookingId`, xử lý chat bình thường (không leak dữ liệu booking).
3. Auth + booking hợp lệ → `FloraContextBuilder` / `FloraJourneyService` như trước.

## Location & privacy

- `latitude` / `longitude` phải đi cùng nhau; phạm vi hợp lệ lat ∈ [-90, 90], lon ∈ [-180, 180].
- Chatbot endpoint **không lưu** GPS vào DB.
- Tọa độ trong prompt LLM được làm tròn 2 chữ số thập phân.
- `FloraPrivacyService.sanitizeForLlm()` loại SĐT, email, số tiền, UUID, dòng thanh toán/refund.

## Logging an toàn

Structured events (không log nội dung tin nhắn, JWT, key OpenRouter, GPS đầy đủ):

- `rate_limit_allowed` / `rate_limit_blocked`
- `chatbot_request_type`
- `authenticated_or_anonymous`
- `booking_context_requested` / `booking_context_denied`
- `llm_fallback_used`

Client key / user id trong log dùng SHA-256 prefix (8 byte hex).

## Rủi ro còn lại (production)

1. **In-memory rate limit** không đồng bộ giữa nhiều instance.
2. **Guest abuse** theo IP — NAT/shared IP có thể bị ảnh hưởng chung quota.
3. **LLM** vẫn phụ thuộc OpenRouter; không có content moderation riêng.
4. **CAPTCHA / device fingerprint** chưa có cho guest.
5. Redis chưa bật trên cloud profile — cần ElastiCache + cấu hình trước khi dùng shared limiter.

## Tương thích API

- Endpoint paths không đổi.
- Request: `content`, `message`, `sessionId`, `state`, `bookingId`, `latitude`, `longitude`, … vẫn chấp nhận.
- Response: `reply` (và `answer` alias) giữ nguyên.
- `userId` trong body vẫn parse được nhưng **không** dùng cho authorization.

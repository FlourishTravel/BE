# Cấu trúc thư mục Backend (Spring Boot)

```
BE/
├── pom.xml
├── README.md
├── STRUCTURE.md
└── src/main/
    ├── java/com/flourishtravel/
    │   ├── FlourishTravelApplication.java
    │   ├── config/           → SecurityConfig, CorsConfig, WebSocketConfig, PasswordEncoderConfig
    │   ├── common/           → BaseEntity, ApiResponse, exception (ResourceNotFound, BadRequest, GlobalHandler)
    │   ├── security/         → JwtProperties, JwtProvider, JwtAuthenticationFilter, JwtEntryPoint, UserPrincipal
    │   ├── chat/websocket/   → ChatWebSocketHandler
    │   └── domain/
    │       ├── user/         → entity (User, Role, UserProvider, RefreshToken), repository, service (CustomUserDetailsService)
    │       ├── auth/         → dto (Login, Register, AuthResponse), service (AuthService), controller (AuthController)
    │       ├── tour/         → entity (Category, Tour, TourSession, TourLocation, TourItinerary, TourImage, TourVideo), repository, service, controller
    │       ├── booking/      → entity (Booking, Promotion), repository
    │       ├── payment/      → entity (Payment, Refund), service (MomoIpnService), controller (MomoWebhookController)
    │       ├── chat/         → entity (ChatRoom, ChatMember, Message, MessageReaction), service, controller
    │       ├── chatbot/     → dto (ChatbotRequest, ChatbotResponse), service, controller
    │       ├── notification/ → entity, repository, service, controller
    │       ├── review/      → entity (Review), repository, service, controller
    │       ├── audit/        → entity (AuditLog), repository
    │       ├── admin/        → dto (AdminStatsResponse), service (AdminStatsService), controller (stats, payments, refunds, contact-requests, audit-logs, tours, sessions, users)
    │       ├── guide/        → service (GuideService), controller
    │       ├── contact/     → entity (ContactRequest), repository, controller
    │       ├── favorite/    → service, controller (User_Favorites)
    │       └── upload/      → controller
    └── resources/
        ├── application.yml
        └── application-dev.yml
```

## Endpoint chính (theo docs/FLOWS.md)

### Auth
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /api/auth/register | Đăng ký |
| POST | /api/auth/login | Đăng nhập |
| POST | /api/auth/oauth | Đăng nhập OAuth (Google/Facebook), body: { provider, id_token } |
| POST | /api/auth/refresh | Refresh token |
| POST | /api/auth/logout | Đăng xuất |
| POST | /api/auth/forgot-password | Quên mật khẩu |
| POST | /api/auth/reset-password | Đặt lại mật khẩu (token) |
| POST | /api/auth/change-password | Đổi mật khẩu (đã đăng nhập) |

### User & Tour
| Method | Path | Mô tả |
|--------|------|--------|
| GET / PATCH | /api/users/me | Hồ sơ cá nhân |
| GET | /api/tours | Danh sách tour (search, filter, sort) |
| GET | /api/tours/:id, /api/tours/by-slug/:slug | Chi tiết tour |
| GET / POST / DELETE | /api/favorites | Wishlist (GET danh sách, POST thêm, DELETE /:tourId) |

### Booking & Payment
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /api/bookings | Tạo đơn (+ guests?: [{fullName, idNumber?, dateOfBirth?}], emergency_contact_name?, emergency_contact_phone?) |
| GET | /api/bookings/me | Chuyến đi của tôi |
| GET | /api/bookings/:id | Chi tiết đơn |
| POST | /api/bookings/:id/cancel | Hủy đơn pending |
| POST | /api/bookings/validate-promo | Kiểm tra mã khuyến mãi |
| POST | /api/bookings/:id/request-refund | Yêu cầu hoàn tiền |
| POST | /api/payments/momo/ipn | Webhook MoMo IPN |

### Chat & Chatbot
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /api/chatbot/message | Trợ lý AI (content, sessionId?, userId?) |
| GET | /api/chat/rooms/:roomId/messages | Lịch sử tin nhắn |
| PATCH | /api/chat/messages/:id/pin, /unpin | Ghim / bỏ ghim (Guide/Admin) |
| POST | /api/chat/messages/:messageId/reactions | Thêm reaction (like/heart) |

### Guide (role TOUR_GUIDE / ADMIN)
| Method | Path | Mô tả |
|--------|------|--------|
| GET | /api/guide/sessions | Lịch công tác (month?, year?, week?) |
| GET | /api/guide/sessions/:id | Chi tiết session |
| GET | /api/guide/sessions/:sessionId/members | Danh sách khách (paid) |
| POST | /api/guide/checkins | Check-in (session_id, user_id, check_in_type) |

### Admin (role ADMIN)
| Method | Path | Mô tả |
|--------|------|--------|
| GET | /api/admin/stats | Dashboard thống kê |
| GET | /api/admin/payments | Danh sách giao dịch |
| POST | /api/admin/refunds | Hoàn tiền (booking_id, amount?, reason?) |
| GET / PATCH | /api/admin/contact-requests | Liên hệ / Lead |
| GET | /api/admin/audit-logs | Nhật ký audit |
| POST / PUT / DELETE | /api/admin/tours | CRUD Tour |
| POST / PUT / DELETE | /api/admin/sessions | CRUD Session (tự tạo ChatRoom) |
| PATCH | /api/admin/users/:id | Cập nhật role, is_active |

### Khác
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /api/contact-requests | Gửi thông tin tư vấn (public) |
| POST | /api/waitlist | Đăng ký chờ lịch (tour_id hoặc session_id) |
| GET / PATCH / POST | /api/notifications | Thông báo (list, đọc 1, đọc tất cả) |
| POST | /api/reviews | Đánh giá sau chuyến (booking_id, rating, comment) |
| POST | /api/upload | Upload file (ảnh/video), cần auth |

WebSocket: **/api/ws/chat**

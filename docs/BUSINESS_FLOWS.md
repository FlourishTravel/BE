# Flourish Travel — Bản đồ luồng nghiệp vụ

> Tài liệu suy ra từ code thực tế (FE `Client/FE/website`, BE `Server/BE`).  
> Cập nhật: 2026-06-27 · Commit tham chiếu: BE `8a71a25`, FE `da46136`

---

## Mục lục

1. [Kiến trúc tổng quan](#1-kiến-trúc-tổng-quan)
2. [Xác thực & phân quyền](#2-xác-thực--phân-quyền)
3. [Luồng User — Khám phá & đặt tour](#3-luồng-user--khám-phá--đặt-tour)
4. [Luồng User — Chuyến đi & chat](#4-luồng-user--chuyến-đi--chat)
5. [Luồng User — Vé, điểm đến, nội dung](#5-luồng-user--vé-điểm-đến-nội-dung)
6. [Luồng User — Tài khoản & khuyến mãi](#6-luồng-user--tài-khoản--khuyến-mãi)
7. [Luồng Flora AI](#7-luồng-flora-ai)
8. [Luồng Admin](#8-luồng-admin)
9. [Luồng Guide (HDV)](#9-luồng-guide-hdv)
10. [Luồng end-to-end](#10-luồng-end-to-end-từ-tạo-tour--hoàn-chuyến)
11. [Máy trạng thái](#11-máy-trạng-thái-state-machines)
12. [Bảng tra API](#12-bảng-tra-api-theo-luồng)
13. [Entity chính](#13-entity-chính)
14. [Gap & hạn chế](#14-gap--hạn-chế-hiện-tại)

---

## 1. Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────┐
│  Client (React)          API Base: /api                           │
│  Client/FE/website/src   VITE_API_URL → flourish backend         │
└───────────────┬─────────────────────────────────────────────────┘
                │ REST + JWT Bearer
                ▼
┌─────────────────────────────────────────────────────────────────┐
│  Server/BE — Spring Boot                                         │
│  domain/* → Controller → Service → Repository → PostgreSQL       │
└─────────────────────────────────────────────────────────────────┘
```

| Portal | Route prefix | Role BE | Role FE |
|--------|--------------|---------|---------|
| Website user | `/`, `/tours`, `/my-journey`, … | `TRAVELER` | `user` |
| Admin | `/admin/*` | `ADMIN` | `admin` |
| Guide | `/guide/*` | `TOUR_GUIDE` | `guide` |

**Repo Git:** `FlourishTravel/BE`, `FlourishTravel/FE`.

---

## 2. Xác thực & phân quyền

### Đăng nhập

| API | Mô tả |
|-----|--------|
| `POST /auth/register` | Tạo TRAVELER |
| `POST /auth/login` | JWT + user.role |
| `POST /auth/refresh` | Làm mới token |

Sau login: `ADMIN` → `/admin` · `TOUR_GUIDE` → `/guide/dashboard` · `TRAVELER` → `/`

### SecurityConfig (tóm tắt)

| Pattern | Quyền |
|---------|--------|
| `GET /tours`, `/destinations`, `/catalog`, `/content`, `/guides`, `/reviews/public`, `/promotions/active` | Public |
| `POST /contact-requests`, `/contact-requests/newsletter` | Public |
| `POST /bookings/validate-session`, `/bookings/validate-promo` | Public |
| `POST /chatbot/message` | Public (rate limit) |
| `/admin/**` | `ROLE_ADMIN` |
| `/guide/**` | `ADMIN` hoặc `TOUR_GUIDE` |
| Còn lại | JWT |

---

## 3. Luồng User — Khám phá & đặt tour

### Sơ đồ

```
/ hoặc /tours
  → GET /tours (+ segment, destination, categoryId, price)
  → /tours/:id
  → POST /bookings/validate-session
  → /checkout/:tourId
  → POST /bookings/validate-promo (optional)
  → POST /bookings
  → MoMo / PayOS redirect
  → sync-from-return / webhook
  → Booking = paid
  → /my-journey
```

### Browse

| Route | API | BE |
|-------|-----|-----|
| `/tours` | `GET /tours?segment&destination&categoryId&minPrice&maxPrice` | `TourService.publicCatalogBrowse` |
| `/tours?segment=domestic\|international\|school\|corporate` | query `segment` | `Tour.marketSegment` |
| `/tours?wishlist=1` | `GET /favorites` + lọc client | `FavoriteController` |
| `/tours/:id` | `GET /tours/{id}`, `/similar` | `TourService.getPublicDetail` |

**FE:** `TourListing.jsx`, `TourDetail.jsx`, `Checkout.jsx`  
**BE:** `TourController`, `BookingController`, `BookingService`

### Thanh toán

| Method | Gateway |
|--------|---------|
| `ewallet` | MoMo — `POST /bookings/{id}/momo-pay-url` |
| `payos` | PayOS — `POST /bookings/{id}/payos-pay-url` |
| `bank`, `card` | Placeholder → `/checkout/result` |

**BookingService.create:** validate session → Booking pending → Payment pending → tăng `currentParticipants` → trả `paymentUrl`.

---

## 4. Luồng User — Chuyến đi & chat

| Route | API | Chức năng |
|-------|-----|-----------|
| `/my-journey` | `GET /bookings/me` | Danh sách đơn |
| `/my-journey/booking/:id` | `GET /bookings/{id}` | Chi tiết |
| | `POST /bookings/{id}/cancel` | Hủy (pending) |
| | `POST /bookings/{id}/request-refund` | Hoàn tiền |
| `/chat/:bookingId` | `GET/POST /chat/bookings/{id}/messages` | Chat nhóm |

**Filter My Journey** (`navConfig.getTripFilterPhase`): upcoming / ongoing / completed / cancelled theo ngày session + status.

**Chat:** chỉ khi booking ∈ `{ paid, confirmed, completed }`. BE tạo `ChatRoom` theo `TourSession`.

**Flora companion (BookingDetail):**
- `GET /flora/bookings/{id}/journey`
- `POST /flora/bookings/{id}/nearby-recommendations`
- `POST /reviews` (feedback sau tour)

---

## 5. Luồng User — Vé, điểm đến, nội dung

### Vé

```
/activities?type=ticket|combo|event → category BE
GET /catalog/tickets?category
/activities/:slug → GET /catalog/tickets/{slug}
Admin: /admin/catalog-tickets → POST /admin/catalog/tickets
```

### Điểm đến

```
/destinations → GET /destinations
/destinations/:slug → GET /destinations/{slug}
Admin: /admin/destinations
```

### CMS

| Route | API type |
|-------|----------|
| `/news` | `GET /content?type=news` |
| `/stories` | `story` |
| `/travel-guide` | `guide` |
| `/help` | `help` + `POST /contact-requests` |
| `/careers` | `career` |
| `/content/:slug` | `GET /content/{slug}` |
| `/our-guides` | `GET /guides` |

**CMS types:** `news`, `story`, `career`, `help`, `guide`

### Liên hệ

| API | Payload |
|-----|---------|
| `POST /contact-requests` | `{ name, email, phone?, message, tourId? }` |
| `POST /contact-requests/newsletter` | `{ email }` |

Admin xử lý: `/admin/contact-requests`

---

## 6. Luồng User — Tài khoản & khuyến mãi

| Route | API |
|-------|-----|
| `/profile` | `GET/PATCH /users/me` |
| `/privacy-settings` | `GET/PATCH /users/me/travel-preferences` |
| `/my-wallet` | `GET /bookings/me` (tổng hợp client) |
| `/my-vouchers` | `GET /promotions/active` |
| `/my-reviews` | `GET /reviews/me` |
| `/my-points` | **Chưa có API** (placeholder) |
| `/notifications` | `GET/PATCH /notifications` |

**Đánh giá:** trang chủ `GET /reviews/featured` · gửi `POST /reviews` · admin duyệt `/admin/reviews`

---

## 7. Luồng Flora AI

### FAB toàn site (`FloatingChatbot`)

- Route: mọi trang trừ `/admin`, `/guide`, `/chat/*`
- API: `POST /chatbot/message`
- Quick actions: `navConfig.FLORA_QUICK_ACTIONS`

### Flora đồng hành

Gắn `bookingId` — xem mục 4.

### Chat HDV

REST `/chat/bookings/{id}/*` — không qua chatbot.

---

## 8. Luồng Admin

### Tạo tour end-to-end

```
POST /tours
POST /admin/sessions (optional)
PUT /tours/admin/{id}/itinerary
PUT /tours/admin/{id}/locations
PUT /tour-operations/sessions/{id}/guide
```

### Bảng màn hình

| Route | API chính |
|-------|-----------|
| `/admin` | `/finance/admin/overview`, `/users/admin/stats` |
| `/admin/tours` | CRUD `/tours`, `/tours/admin` |
| `/admin/tours/itinerary/:id` | itinerary + locations + geocode |
| `/admin/categories` | `/categories` |
| `/admin/dispatch` | `/tour-operations/*` |
| `/admin/bookings` | `/bookings/admin`, refund approve |
| `/admin/customers` | `/users/admin/customers` |
| `/admin/financials` | `/finance/admin/*` |
| `/admin/promotions` | `/admin/promotions` |
| `/admin/catalog-tickets` | `/admin/catalog/tickets` |
| `/admin/destinations` | `/admin/destinations` |
| `/admin/content` | `/admin/content` |
| `/admin/reviews` | `/admin/reviews` |
| `/admin/notifications` | broadcast |
| `/admin/contact-requests` | PATCH status |
| `/admin/guide-expenses` | duyệt chi phí HDV |
| `/admin/staff` | `/users/admin/staff` |

### Hoàn tiền

```
User POST /bookings/{id}/request-refund → Refund pending
Admin approve → booking cancelled, trả slot session
```

---

## 9. Luồng Guide (HDV)

| Route | API |
|-------|-----|
| `/guide/dashboard` | `GET /guide/sessions` |
| `/guide/tours/:tourId`* | `GET /guide/sessions/{id}`, schedule, check-in |
| `/guide/guests` | guests, check-in/out |
| `/guide/communication` | `/chat/bookings/{id}/messages` |
| `/guide/operations` | schedule read-only |
| `/guide/expenses` | `POST .../expenses` → admin duyệt |

*Param `:tourId` thực tế là **sessionId** (UUID).

---

## 10. Luồng end-to-end: từ tạo tour → hoàn chuyến

```
[ADMIN]  Tạo tour → session → itinerary → gán HDV → CMS/promo/destinations
[USER]   Browse → checkout → paid → my-journey → chat
[GUIDE]  Check-in → schedule → chat → chi phí
[USER]   POST /reviews
[ADMIN]  Duyệt review → featured trang chủ
[FLORA]  PATCH preferences → cá nhân hóa lần sau
```

---

## 11. Máy trạng thái (state machines)

### Booking

```
pending → paid → confirmed → completed
   │        │         │
   └────────┴─────────┴──→ cancelled
```

| From | To |
|------|-----|
| pending | paid, cancelled |
| paid | confirmed, cancelled, completed |
| confirmed | completed, cancelled |

### Payment

`pending` → `paid` (MoMo/PayOS/admin mark-paid)

### Refund

`(none)` → `pending` → `approved` | `rejected`

### TourSession

`scheduled` → `completed` | `cancelled`

### Review

`isPublished=false` → admin publish → public / featured

### ContactRequest

`new` → `in_progress` → `resolved`

### GuideSessionExpense

`pending` → `approved` | `rejected`

---

## 12. Bảng tra API theo luồng

### User — Tour & Booking

| API | Auth |
|-----|------|
| `GET /tours`, `/tours/{id}` | Public |
| `GET/POST/DELETE /favorites` | JWT |
| `POST /bookings/validate-session`, `/validate-promo` | Public |
| `POST /bookings`, `GET /bookings/me`, `GET /bookings/{id}` | JWT |
| `POST /bookings/{id}/cancel`, `/request-refund` | JWT |

### Catalog & Destination

| API | Auth |
|-----|------|
| `GET /catalog/tickets`, `/catalog/tickets/{slug}` | Public |
| `GET /destinations`, `/destinations/{slug}` | Public |

### Account & Content

| API | Auth |
|-----|------|
| `POST /auth/login` | Public |
| `GET/PATCH /users/me` | JWT |
| `GET /promotions/active`, `/reviews/featured` | Public |
| `GET /reviews/me`, `POST /reviews` | JWT |
| `GET /content?type=`, `/guides` | Public |
| `POST /contact-requests` | Public |

---

## 13. Entity chính

| Entity | Vai trò |
|--------|---------|
| `Tour` | Sản phẩm (marketSegment, destinationCity) |
| `TourSession` | Lịch khởi hành |
| `TourItinerary` | Lịch trình ngày + activities |
| `Booking` | Đơn đặt user |
| `Payment` | Thanh toán |
| `Refund` | Hoàn tiền |
| `Promotion` | Mã giảm giá |
| `Favorite` | Wishlist |
| `Destination` | Điểm đến marketing |
| `TravelTicket` | Vé catalog |
| `SiteContent` | CMS |
| `Review` | Đánh giá |
| `Notification` | Thông báo |
| `ContactRequest` | Liên hệ / newsletter |
| `ChatRoom`, `Message` | Chat nhóm |
| `GuideSessionExpense` | Chi phí HDV |
| `User` | TRAVELER / TOUR_GUIDE / ADMIN / STAFF |

**Quan hệ chính:** Tour → TourSession → Booking → Payment. TourSession → ChatRoom. User → Favorite → Tour. Booking → Review.

---

## 14. Gap & hạn chế hiện tại

| # | Mục | Trạng thái |
|---|-----|------------|
| 1 | Điểm thưởng `/my-points` | Chưa có API BE |
| 2 | Thanh toán bank/card | Placeholder |
| 3 | Guide settings route | Chưa define trong App.jsx |
| 4 | Admin Settings | Placeholder → dùng CMS |
| 5 | Waitlist | BE có, FE chưa |
| 6 | Planner `/planner/*` | BE public, FE chưa wire |

### Đã hoàn thiện luồng (2026-06-27)

| Gap | Fix |
|-----|-----|
| Contact API path | `contact.js` → `/contact-requests`, map `fullName` → `name` |
| Reviews API thiếu | `reviews.js` → `listFeaturedReviews`, `createReview` |

---

## Phụ lục: Route map FE

### User

```
/  /help  /login  /register  /profile  /privacy-settings
/my-journey  /my-journey/booking/:bookingId
/my-wallet  /my-vouchers  /my-reviews  /my-points
/destinations  /destinations/:slug  /travel-guide
/our-guides  /our-guides/:id  /notifications
/tours  /tours/:id  /activities  /activities/:slug
/checkout/:tourId  /checkout/result  /chat/:bookingId
/news  /stories  /content/:slug  /careers  /about  /policies...
```

### Admin

```
/admin  /admin/tours  /admin/tours/itinerary/:tourId
/admin/categories  /admin/dispatch  /admin/bookings
/admin/customers  /admin/financials  /admin/promotions
/admin/catalog-tickets  /admin/destinations  /admin/content
/admin/reviews  /admin/notifications  /admin/contact-requests
/admin/guide-expenses  /admin/staff  /admin/settings
```

### Guide

```
/guide/dashboard  /guide/tours  /guide/tours/:tourId
/guide/guests  /guide/communication  /guide/operations  /guide/expenses
```

---

*Tham chiếu thêm: `STRUCTURE.md`, `docs/flora-ai/overview.md`*

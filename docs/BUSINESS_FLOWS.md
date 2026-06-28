# Flourish Travel — Sơ đồ luồng nghiệp vụ

> Tài liệu trực quan suy ra từ code FE + BE.  
> File nguồn Mermaid: [`docs/business-flows/*.mmd`](./business-flows/)  
> Cập nhật: 2026-06-27

**Cách xem:** GitHub/GitLab render Mermaid trực tiếp. VS Code/Cursor: cài extension *Markdown Preview Mermaid Support* hoặc mở file `.mmd` trên [mermaid.live](https://mermaid.live).

---

## Mục lục

| # | Sơ đồ | File nguồn |
|---|--------|------------|
| 1 | [Kiến trúc tổng quan](#1-kiến-trúc-tổng-quan) | `01-overview.mmd` |
| 2 | [Auth & phân quyền](#2-auth--phân-quyền) | `02-auth-roles.mmd` |
| 3 | [User — Khám phá tour](#3-user--khám-phá-tour) | `03-user-browse-tour.mmd` |
| 4 | [User — Đặt tour & thanh toán](#4-user--đặt-tour--thanh-toán) | `04-user-booking-payment.mmd` |
| 5 | [User — Chuyến đi của tôi](#5-user--chuyến-đi-của-tôi) | `05-user-my-journey.mmd` |
| 6 | [User — Vé, điểm đến, CMS](#6-user--vé-điểm-đến-cms) | `06-user-catalog-destination.mmd` |
| 7 | [User — Tài khoản](#7-user--tài-khoản) | `07-user-account.mmd` |
| 8 | [Flora AI](#8-flora-ai) | `08-flora-ai.mmd` |
| 9 | [Admin — Vòng đời tour](#9-admin--vòng-đời-tour) | `09-admin-tour-lifecycle.mmd` |
| 10 | [Admin — Vận hành](#10-admin--vận-hành) | `10-admin-operations.mmd` |
| 11 | [Guide — Portal HDV](#11-guide--portal-hdv) | `11-guide-portal.mmd` |
| 12 | [Máy trạng thái](#12-máy-trạng-thái) | `12-state-machines.mmd` |
| 13 | [End-to-end](#13-end-to-end) | `13-end-to-end.mmd` |
| 14 | [Quan hệ entity](#14-quan-hệ-entity) | `14-entity-relations.mmd` |

---

## 1. Kiến trúc tổng quan

Ba portal FE kết nối REST API `/api`, xử lý bởi Spring Boot → PostgreSQL.

```mermaid
flowchart TB
    subgraph Clients["Client (React)"]
        WEB["Website User\n/"]
        ADMIN["Admin Panel\n/admin/*"]
        GUIDE["Guide Portal\n/guide/*"]
    end

    subgraph API["REST API /api"]
        AUTH["/auth"]
        TOUR["/tours"]
        BOOK["/bookings"]
        CAT["/catalog"]
        DEST["/destinations"]
        CHAT["/chat"]
        FLORA["/chatbot + /flora"]
        CMS["/content"]
    end

    subgraph BE["Spring Boot Services"]
        SVC["Controller → Service → Repository"]
    end

    DB[(PostgreSQL)]

    WEB --> AUTH & TOUR & BOOK & CAT & DEST & CHAT & FLORA & CMS
    ADMIN --> TOUR & BOOK & CMS
    GUIDE --> CHAT

    AUTH & TOUR & BOOK & CAT & DEST & CHAT & FLORA & CMS --> SVC
    SVC --> DB

    classDef user fill:#e0f2fe,stroke:#0284c7
    classDef admin fill:#fef3c7,stroke:#d97706
    classDef guide fill:#dcfce7,stroke:#16a34a
    class WEB user
    class ADMIN admin
    class GUIDE guide
```

---

## 2. Auth & phân quyền

| Role BE | Role FE | Sau login |
|---------|---------|-----------|
| `ADMIN` | `admin` | `/admin` |
| `TOUR_GUIDE` | `guide` | `/guide/dashboard` |
| `TRAVELER` | `user` | `/` |

```mermaid
flowchart LR
    subgraph Login["POST /auth/login"]
        L1["email + password"]
    end

    subgraph Roles["JWT + Role"]
        R1["ADMIN"]
        R2["TOUR_GUIDE"]
        R3["TRAVELER"]
    end

    subgraph Redirect["FE Redirect"]
        D1["/admin"]
        D2["/guide/dashboard"]
        D3["/"]
    end

    L1 --> R1 & R2 & R3
    R1 --> D1
    R2 --> D2
    R3 --> D3

    subgraph Guards["Route Guards"]
        G1["ProtectedAdminRoute"]
        G2["ProtectedGuideRoute"]
        G3["AuthContext user"]
    end

    D1 --> G1
    D2 --> G2
    D3 --> G3
```

---

## 3. User — Khám phá tour

```mermaid
flowchart TD
    START(["User vào website"])

    START --> HOME["/ Trang chủ"]
    START --> LIST["/tours"]

    HOME --> API1["GET /tours?size=10"]
    LIST --> FILTERS{"Bộ lọc"}

    FILTERS --> F1["?segment=domestic\ninternational\nschool\|corporate"]
    FILTERS --> F2["?destination=slug"]
    FILTERS --> F3["categoryId + min/maxPrice"]
    FILTERS --> F4["?wishlist=1"]

    F1 --> API2["GET /tours?segment=..."]
    F2 --> API2
    F3 --> API2
    F4 --> API2b["GET /favorites\n→ lọc client"]

    API1 --> CARDS["Tour cards"]
    API2 --> CARDS
    API2b --> CARDS

    CARDS --> FAV{"Bấm ♥?"}
    FAV -->|Chưa login| ALERT["Alert đăng nhập"]
    FAV -->|Đã login| API3["POST/DELETE /favorites"]

    CARDS --> DETAIL["/tours/:id"]
    DETAIL --> API4["GET /tours/{id}"]
    DETAIL --> API5["GET /tours/{id}/similar"]
```

---

## 4. User — Đặt tour & thanh toán

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant TD as TourDetail
    participant CK as Checkout
    participant BE as BookingService
    participant GW as MoMo / PayOS
    participant MJ as My Journey

    U->>TD: Chọn session + số khách
    TD->>BE: POST /bookings/validate-session
    BE-->>TD: valid ✓

    U->>CK: /checkout/:tourId
    CK->>BE: POST /bookings/validate-promo (optional)
    U->>CK: Submit đặt tour
    CK->>BE: POST /bookings

    Note over BE: Booking status=pending<br/>Payment status=pending<br/>session.currentParticipants++

    BE-->>CK: paymentUrl

    alt ewallet (MoMo)
        CK->>GW: Redirect MoMo
    else payos
        CK->>GW: Redirect PayOS
    else bank/card
        CK->>CK: /checkout/result (placeholder)
    end

    GW->>BE: Webhook / sync-from-return
    Note over BE: Booking status=paid

    U->>MJ: /my-journey
    MJ->>BE: GET /bookings/me
```

---

## 5. User — Chuyến đi của tôi

```mermaid
flowchart TD
    MJ["/my-journey"] --> API["GET /bookings/me"]
    API --> LIST["Danh sách booking"]

    LIST --> FILTER{"Filter chip"}
    FILTER --> UP["Sắp khởi hành"]
    FILTER --> ON["Đang diễn ra"]
    FILTER --> DONE["Đã hoàn thành"]
    FILTER --> CAN["Đã hủy"]

    LIST --> DETAIL["/my-journey/booking/:id"]
    DETAIL --> API2["GET /bookings/{id}"]

    DETAIL --> ACT{"Hành động"}
    ACT --> CANCEL["POST .../cancel\n(chỉ pending)"]
    ACT --> REFUND["POST .../request-refund\n(paid, trước ngày đi)"]
    ACT --> FLORA["Flora Companion\nGET /flora/.../journey"]
    ACT --> REVIEW["POST /reviews\n(feedback sau tour)"]

    DETAIL --> CHAT{"Status paid/confirmed/completed?"}
    CHAT -->|Yes| CHATPAGE["/chat/:bookingId"]
    CHATPAGE --> MSG["GET/POST /chat/bookings/{id}/messages"]

    CANCEL --> S1["Booking → cancelled\ntrả slot session"]
    REFUND --> S2["Refund → pending\nAdmin duyệt sau"]
```

---

## 6. User — Vé, điểm đến, CMS

```mermaid
flowchart LR
    subgraph Vé["Vé & Hoạt động"]
        A1["/activities"] --> A2["GET /catalog/tickets?category"]
        A3["/activities/:slug"] --> A4["GET /catalog/tickets/{slug}"]
        A5["?type=ticket"] --> CAT1["category=attraction"]
        A6["?type=combo"] --> CAT2["category=combo"]
        A7["?type=event"] --> CAT3["category=show"]
    end

    subgraph DD["Điểm đến"]
        D1["/destinations"] --> D2["GET /destinations"]
        D3["/destinations/:slug"] --> D4["GET /destinations/{slug}"]
        D4 --> D5["/tours?destination=slug"]
    end

    subgraph CMS["Nội dung CMS"]
        C1["/news /stories /help\ntravel-guide /careers"] --> C2["GET /content?type="]
        C3["/content/:slug"] --> C4["GET /content/{slug}"]
        C5["/our-guides"] --> C6["GET /guides"]
    end

    subgraph Contact["Liên hệ"]
        H1["/help form"] --> H2["POST /contact-requests"]
        F1["Footer newsletter"] --> F2["POST /contact-requests/newsletter"]
        H2 --> ADM["Admin /admin/contact-requests"]
    end
```

---

## 7. User — Tài khoản

```mermaid
flowchart TB
    subgraph Profile["Menu Profile (navConfig)"]
        P1["/profile"] --> API1["GET/PATCH /users/me"]
        P2["/my-journey"] --> API2["GET /bookings/me"]
        P3["/tours?wishlist=1"] --> API3["GET /favorites"]
        P4["/my-wallet"] --> API2
        P5["/my-vouchers"] --> API4["GET /promotions/active"]
        P6["/my-reviews"] --> API5["GET /reviews/me"]
        P7["/my-points"] --> PLACE["Placeholder\n(chưa có API)"]
        P8["/privacy-settings"] --> API6["travel-preferences"]
        P9["/notifications"] --> API7["GET /notifications"]
    end

    subgraph Public["Không cần login"]
        P5
        HOME["/ FeaturedReviews"] --> API8["GET /reviews/featured"]
    end
```

---

## 8. Flora AI

Hai kênh tách biệt: **FAB toàn site** (chatbot) và **Companion gắn booking**.

```mermaid
flowchart TB
    subgraph FAB["Flora FAB (FloatingChatbot)"]
        F1["Mọi trang trừ\n/admin /guide /chat/*"]
        F2["POST /chatbot/message"]
        F3["Quick actions\nnavConfig.FLORA_QUICK_ACTIONS"]
    end

    subgraph Companion["Flora Companion (BookingDetail)"]
        C1["/my-journey/booking/:id"]
        C2["GET /flora/bookings/{id}/journey"]
        C3["POST .../nearby-recommendations"]
        C4["GET .../post-tour-feedback"]
        C5["POST /reviews"]
        C6["PATCH /flora/preferences/me"]
    end

    subgraph ChatHDV["Chat HDV (không qua chatbot)"]
        H1["/chat/:bookingId"]
        H2["GET/POST /chat/bookings/{id}/messages"]
    end

    USER((User)) --> FAB
    USER --> Companion
    USER --> ChatHDV

    F1 --> F2
    F1 --> F3
    C1 --> C2 & C3 & C4 & C5 & C6
```

Chi tiết Flora MVP: [`docs/flora-ai/use-case-diagram.mmd`](../flora-ai/use-case-diagram.mmd)

---

## 9. Admin — Vòng đời tour

```mermaid
flowchart TD
    A1["/admin/tours\nCreateTourModal"] --> A2["POST /tours\n+ marketSegment\n+ destinationCity"]
    A2 --> A3["POST /admin/sessions\n(lịch khởi hành)"]
    A3 --> A4["/admin/tours/itinerary/:tourId"]

    A4 --> A5["PUT /tours/admin/{id}/itinerary\nngày + activities"]
    A4 --> A6["PUT /tours/admin/{id}/locations\nđịa điểm theo ngày"]
    A4 --> A7["GET geocode\nVietMap"]

    A5 --> PUB["Tour public\nGET /tours"]
    A6 --> PUB

    subgraph Dispatch["Điều hành"]
        D1["/admin/dispatch"] --> D2["GET /tour-operations/sessions"]
        D2 --> D3["PUT .../guide\nGán TOUR_GUIDE"]
        D3 --> D4["Guide portal\nGET /guide/sessions"]
    end

    PUB --> D1

    subgraph Content["Nội dung marketing"]
        M1["/admin/destinations"]
        M2["/admin/catalog-tickets"]
        M3["/admin/promotions"]
        M4["/admin/content CMS"]
    end
```

---

## 10. Admin — Vận hành

```mermaid
flowchart LR
    subgraph Bookings["Quản lý đặt chỗ"]
        B1["/admin/bookings"] --> B2["GET /bookings/admin"]
        B2 --> B3["PATCH status"]
        B2 --> B4["POST mark-paid"]
        B2 --> B5["Approve/Reject refund"]
    end

    subgraph Finance["Tài chính"]
        F1["/admin/financials"] --> F2["GET /finance/admin/overview"]
        F2 --> F3["Transactions + Export CSV"]
    end

    subgraph People["Con người"]
        P1["/admin/customers"] --> P2["TRAVELER users"]
        P3["/admin/staff"] --> P4["ADMIN/GUIDE/STAFF"]
        P5["/admin/reviews"] --> P6["Publish / Featured"]
        P7["/admin/notifications"] --> P8["Broadcast"]
    end

    subgraph Expense["Chi phí HDV"]
        E1["Guide POST expenses"] --> E2["/admin/guide-expenses"]
        E2 --> E3["Approve / Reject"]
    end
```

---

## 11. Guide — Portal HDV

```mermaid
flowchart TD
    ADMIN["Admin /admin/dispatch\nGán HDV"] --> SESSION["TourSession\n.tourGuide = HDV"]

    SESSION --> GD["/guide/dashboard"]
    GD --> API["GET /guide/sessions"]

    API --> TOURS["/guide/tours"]
    API --> GUESTS["/guide/guests"]
    API --> OPS["/guide/operations"]
    API --> EXP["/guide/expenses"]
    API --> COMM["/guide/communication"]

    TOURS --> TD["/guide/tours/:sessionId"]
    TD --> S1["GET session detail"]
    TD --> S2["Schedule CRUD"]
    TD --> S3["Guide check-in"]

    GUESTS --> G1["GET .../guests"]
    G1 --> G2["Participant check-in/out"]
    G1 --> G3["Activity check-in/out"]

    COMM --> C1["GET/POST /chat/bookings/{id}/messages"]

    EXP --> E1["POST .../expenses\nstatus=pending"]
    E1 --> E2["Admin duyệt\n/admin/guide-expenses"]
```

---

## 12. Máy trạng thái

```mermaid
stateDiagram-v2
    direction LR

    state "Booking" as booking {
        [*] --> pending
        pending --> paid: Thanh toán / admin mark-paid
        pending --> cancelled: User cancel / admin
        paid --> confirmed: Admin xác nhận
        paid --> completed: Kết thúc tour
        paid --> cancelled: Hủy / refund approved
        confirmed --> completed: Ngày kết thúc
        confirmed --> cancelled: Hủy / refund
        completed --> [*]
        cancelled --> [*]
    }

    state "Payment" as payment {
        [*] --> p_pending: Tạo booking
        p_pending --> p_paid: MoMo/PayOS IPN
        p_pending --> p_failed: Timeout/lỗi
        p_paid --> [*]
        p_failed --> [*]
    }

    state "Refund" as refund {
        [*] --> r_pending: User request-refund
        r_pending --> r_approved: Admin approve
        r_pending --> r_rejected: Admin reject
        r_approved --> [*]: booking cancelled
        r_rejected --> [*]
    }

    state "Review" as review {
        [*] --> draft: POST /reviews
        draft --> published: Admin publish
        published --> featured: Admin featured
        featured --> [*]
        published --> [*]
    }
```

---

## 13. End-to-end

Luồng đầy đủ từ admin tạo tour đến user đánh giá sau chuyến.

```mermaid
flowchart TB
    subgraph Phase1["① Admin chuẩn bị"]
        A1["Tạo Tour"] --> A2["Tạo Session"]
        A2 --> A3["Itinerary + Locations"]
        A3 --> A4["Gán HDV"]
        A4 --> A5["CMS / Promo / Destinations"]
    end

    subgraph Phase2["② User đặt tour"]
        U1["Browse /tours"] --> U2["TourDetail"]
        U2 --> U3["Checkout"]
        U3 --> U4["Thanh toán"]
        U4 --> U5["Booking = paid"]
    end

    subgraph Phase3["③ Trong chuyến"]
        G1["Guide check-in"] --> G2["Chat nhóm"]
        G2 --> G3["Flora journey companion"]
        U6["User /my-journey"] --> G2
    end

    subgraph Phase4["④ Sau chuyến"]
        S1["Tour completed"] --> S2["User POST /reviews"]
        S2 --> S3["Admin publish"]
        S3 --> S4["Featured trang chủ"]
        S4 --> S5["Flora cập nhật preferences"]
    end

    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase3 --> Phase4

    A5 -.-> U1
    A4 -.-> G1
```

---

## 14. Quan hệ entity

```mermaid
erDiagram
    User ||--o{ Booking : places
    User ||--o{ Favorite : saves
    User ||--o{ Review : writes
    User ||--o{ Notification : receives

    Tour ||--o{ TourSession : has
    Tour ||--o{ TourItinerary : has
    Tour }o--|| Category : belongs
    Tour ||--o{ Favorite : referenced

    TourSession ||--o{ Booking : receives
    TourSession |o--o| User : tourGuide
    TourSession ||--o| ChatRoom : has
    TourSession ||--o{ GuideSessionExpense : has

    Booking ||--o{ BookingGuest : has
    Booking ||--o| Payment : has
    Booking ||--o| Refund : may_have
    Booking ||--o| Review : generates

    ChatRoom ||--o{ Message : contains
    ChatRoom ||--o{ ChatMember : has

    Promotion }o--o{ Booking : applied
    Destination ||--o{ Tour : suggests
    TravelTicket }o--|| Catalog : listed
    SiteContent }o--|| CMS : typed
    ContactRequest }o--o| User : from
```

---

## Phụ lục — Route map

<details>
<summary>User routes (click mở)</summary>

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

</details>

<details>
<summary>Admin routes</summary>

```
/admin  /admin/tours  /admin/tours/itinerary/:tourId
/admin/categories  /admin/dispatch  /admin/bookings
/admin/customers  /admin/financials  /admin/promotions
/admin/catalog-tickets  /admin/destinations  /admin/content
/admin/reviews  /admin/notifications  /admin/contact-requests
/admin/guide-expenses  /admin/staff  /admin/settings
```

</details>

<details>
<summary>Guide routes</summary>

```
/guide/dashboard  /guide/tours  /guide/tours/:tourId
/guide/guests  /guide/communication  /guide/operations  /guide/expenses
```

</details>

---

*Tham chiếu: [`STRUCTURE.md`](../../STRUCTURE.md) · [`docs/flora-ai/overview.md`](../flora-ai/overview.md)*

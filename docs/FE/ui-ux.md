# LAK — UI/UX Specification

> **Phiên bản:** 2.0  
> **Phạm vi:** Website khách hàng, trang quản trị và giao diện quét vé  
> **Phong cách:** Clean, professional, light cinema  
> **Mục tiêu sử dụng:** Source of truth cho thiết kế UI/UX, frontend implementation, coding agent và stakeholder review  
> **Nguồn:** `docs/overview.md`, `docs/BE/design-systems.md`, phiên bản UI/UX trước đó và các quyết định UX đã chốt trong quá trình thiết kế.

---

# 1. Mục đích tài liệu

Tài liệu này định nghĩa cách hệ thống LAK phải **trông như thế nào, hành xử như thế nào và phản hồi như thế nào** trên các màn hình chính.

Tài liệu trả lời bốn câu hỏi:

1. Hệ thống có những màn hình nào?
2. Mỗi màn hình được bố trí như thế nào?
3. Các component tương tác ra sao?
4. UI phải phản ứng thế nào trước loading, lỗi, conflict, timeout và thay đổi trạng thái nghiệp vụ?

Tài liệu này không thay thế system design hoặc API contract. Backend vẫn là nguồn quyết định đối với:

- trạng thái ghế;
- giá vé;
- booking;
- payment;
- refund;
- ticket;
- authorization.

Frontend chỉ trình bày và tương tác với các trạng thái do backend cung cấp.

---

# 2. UX Principles

## 2.1. Booking-first

Luồng chính phải giúp người dùng đi từ:

```text
Chọn phim
→ Chọn rạp / suất chiếu
→ Chọn ghế
→ Checkout
→ Thanh toán
→ Nhận vé
```

với số bước và lượng nhập liệu tối thiểu.

Không tạo các bước trung gian không mang giá trị nghiệp vụ.

---

## 2.2. State clarity

Các trạng thái quan trọng phải luôn dễ nhận biết:

- ghế còn trống;
- ghế đang giữ;
- ghế đã bán;
- booking chờ thanh toán;
- payment đang xác nhận;
- payment thành công;
- payment thất bại;
- refund đang xử lý;
- ticket hợp lệ hoặc đã sử dụng.

Không truyền đạt trạng thái chỉ bằng màu sắc.

---

## 2.3. Price transparency

Người dùng phải biết rõ:

- giá từng ghế;
- phụ thu;
- voucher;
- giảm giá;
- phí dịch vụ;
- tổng tiền.

Không xuất hiện chi phí bất ngờ ở bước cuối.

Giá authoritative luôn lấy từ backend.

---

## 2.4. Failure-safe UX

Retry, refresh hoặc mất kết nối không được làm:

- giữ ghế trùng;
- tạo booking trùng;
- tạo payment trùng;
- kéo dài hold trái quy tắc;
- khiến người dùng hiểu sai trạng thái thanh toán.

Khi trạng thái chưa chắc chắn, UI phải trình bày đúng sự không chắc chắn đó.

---

## 2.5. Responsive by design

Mobile không phải desktop thu nhỏ.

Các layout phức tạp phải có cách trình bày phù hợp mobile, ví dụ:

- filter → drawer;
- booking sidebar → bottom sheet;
- seat map → pan/zoom;
- CTA quan trọng → sticky bottom action.

---

## 2.6. Accessibility by default

Accessibility là Definition of Done, không phải phần polish cuối dự án.

Mọi màn hình P0 phải hỗ trợ:

- keyboard;
- focus visible;
- semantic HTML;
- screen reader;
- touch target phù hợp;
- contrast phù hợp;
- reduced motion.

---

# 3. Vai trò người dùng

LAK có bốn nhóm vai trò UI chính.

| Vai trò | Mục tiêu chính |
|---|---|
| Public visitor | Khám phá phim, rạp và suất chiếu |
| Customer | Đặt vé, thanh toán, xem vé, refund |
| Ticket Staff | Quét và xác thực vé |
| Cinema Manager / Super Admin | Quản lý vận hành và dữ liệu |

Frontend guard chỉ cải thiện UX. Authorization thật luôn được kiểm tra ở backend.

---

# 4. Information Architecture

## 4.1. Customer

```text
Home
├── Movies
│   ├── Now Showing
│   ├── Coming Soon
│   └── Movie Detail
│       └── Showtime Selection
│           └── Seat Selection
│               └── Checkout
│                   └── Payment Result
│
├── Cinemas
│
└── Account
    ├── My Tickets
    ├── Booking History
    ├── Booking Detail
    ├── Refund Request
    └── Profile
```

---

## 4.2. Staff

```text
Staff
├── QR Scanner
├── Manual Ticket Lookup
└── Scan History
```

---

## 4.3. Admin

```text
Dashboard
├── Catalog
│   ├── Movies
│   └── Genres
├── Cinema Operations
│   ├── Cinemas
│   ├── Auditoriums
│   ├── Seats
│   └── Showtimes
├── Commerce
│   ├── Pricing
│   ├── Bookings
│   ├── Payments
│   ├── Refunds
│   └── Vouchers
├── Users
└── Reporting
```

---

# 5. Screen Inventory

Routes dưới đây là contract frontend đề xuất cho implementation.

## 5.1. Customer screens

| ID | Screen | Route | Auth | Priority |
|---|---|---|---|---|
| C01 | Home | `/` | No | P0 |
| C02 | Movie List | `/movies` | No | P0 |
| C03 | Movie Detail | `/movies/:id` | No | P0 |
| C04 | Showtime Selection | `/movies/:id/showtimes` | No | P0 |
| C05 | Seat Selection | `/showtimes/:id/seats` | Yes | P0 |
| C06 | Checkout | `/checkout/:bookingId` | Yes | P0 |
| C07 | Payment Result | `/payments/:id/result` | Yes | P0 |
| C08 | My Tickets | `/me/tickets` | Yes | P0 |
| C09 | Booking Detail | `/me/bookings/:code` | Yes | P1 |
| C10 | Refund Request | `/me/bookings/:id/refund` | Yes | P1 |
| C11 | Login | `/login` | No | P0 |
| C12 | Register | `/register` | No | P0 |
| C13 | Forgot Password | `/forgot-password` | No | P1 |
| C14 | Reset Password | `/reset-password` | No | P1 |
| C15 | Profile | `/me/profile` | Yes | P1 |

---

## 5.2. Staff screens

| ID | Screen | Route | Priority |
|---|---|---|---|
| S01 | QR Scanner | `/staff/scanner` | P1 |
| S02 | Manual Ticket Lookup | `/staff/tickets` | P1 |
| S03 | Scan History | `/staff/scans` | P1 |

---

## 5.3. Admin screens

| ID | Screen | Route | Priority |
|---|---|---|---|
| A01 | Dashboard | `/admin` | P1 |
| A02 | Movies | `/admin/movies` | P1 |
| A03 | Cinemas | `/admin/cinemas` | P1 |
| A04 | Auditoriums & Seats | `/admin/auditoriums` | P1 |
| A05 | Showtimes | `/admin/showtimes` | P1 |
| A06 | Pricing | `/admin/pricing` | P1 |
| A07 | Bookings | `/admin/bookings` | P1 |
| A08 | Payments | `/admin/payments` | P1 |
| A09 | Refunds | `/admin/refunds` | P1 |
| A10 | Vouchers | `/admin/vouchers` | P1 |
| A11 | Users | `/admin/users` | P1 |
| A12 | Reports | `/admin/reports` | P1 |

---

# 6. Visual Direction

LAK sử dụng phong cách **light cinema**:

- nền sáng;
- nhiều khoảng trắng;
- card tối giản;
- typography rõ;
- ảnh phim tạo cảm giác điện ảnh;
- màu đỏ dùng làm brand accent và CTA;
- không sử dụng nền tối toàn trang.

Mục tiêu:

- nhanh;
- rõ;
- đáng tin cậy;
- hiện đại;
- không tạo cảm giác quá nặng hoặc quá nhiều hiệu ứng.

---

# 7. Design Tokens

## 7.1. Color

| Token | Value | Usage |
|---|---|---|
| `primary` | `#E11D48` | CTA, brand, selected seat |
| `primary-hover` | `#BE123C` | Hover |
| `primary-soft` | `#FFF1F2` | Active navigation, badge |
| `background` | `#F8FAFC` | Page background |
| `surface` | `#FFFFFF` | Card, modal, header |
| `text-primary` | `#0F172A` | Heading, main content |
| `text-secondary` | `#475569` | Supporting content |
| `text-muted` | `#94A3B8` | Metadata, placeholder |
| `border` | `#E2E8F0` | Border |
| `success` | `#16A34A` | Success |
| `warning` | `#D97706` | Warning |
| `error` | `#DC2626` | Error |
| `info` | `#2563EB` | Informational state |

Primary chỉ dùng cho hành động quan trọng và brand accent.

---

## 7.2. Typography

```css
font-family: "Be Vietnam Pro", Inter, system-ui, sans-serif;
```

| Level | Size | Weight |
|---|---:|---:|
| Display | `40/48px` | 700 |
| H1 | `32/40px` | 700 |
| H2 | `24/32px` | 600 |
| H3 | `20/28px` | 600 |
| Body | `16/24px` | 400 |
| Small | `14/20px` | 400–500 |
| Caption | `12/16px` | 500 |

Mobile H1:

```text
28–30px
```

Không dùng quá ba mức font-weight trên cùng một screen nếu không cần thiết.

---

## 7.3. Spacing

Hệ spacing:

```text
4
8
12
16
24
32
48
64
```

Không tạo spacing ngẫu nhiên ngoài hệ trên trừ trường hợp component đặc biệt.

---

## 7.4. Radius

| Component | Radius |
|---|---:|
| Input | `8px` |
| Button | `8px` |
| Card | `12px` |
| Modal | `16px` |
| Drawer | `16px` |
| Badge | `999px` |

---

## 7.5. Shadow

Shadow chỉ dùng khi:

- hover card;
- modal;
- dropdown;
- sticky/floating layer.

Không dùng shadow dày cho card thông thường.

---

# 8. Layout System

## 8.1. Global

- Max container: `1200–1280px`.
- Desktop: 12-column grid.
- Desktop gutter: `24px`.
- Tablet gutter: `20px`.
- Mobile horizontal padding: `16px`.
- Desktop header: `72px`.
- Mobile header: `64px`.

---

## 8.2. Breakpoints

| Breakpoint | Target |
|---|---|
| `< 640px` | Mobile |
| `640–767px` | Large mobile |
| `768–1023px` | Tablet |
| `1024–1279px` | Desktop |
| `≥ 1280px` | Large desktop |

---

# 9. Navigation

## 9.1. Customer header

Desktop:

```text
Logo | Phim | Rạp | Tìm kiếm | Vé của tôi | Tài khoản
```

Yêu cầu:

- sticky nhẹ khi scroll;
- chi nhánh đang chọn hiển thị rõ;
- CTA hoặc navigation active không dùng quá nhiều accent màu.

Mobile:

```text
Logo | Search | Tickets | Menu
```

---

## 9.2. Admin navigation

Sidebar khoảng `240px`.

```text
Dashboard
Phim
Rạp & phòng
Suất chiếu
Bảng giá
Booking
Thanh toán
Hoàn tiền
Voucher
Người dùng
Báo cáo
```

Active item dùng `primary-soft`.

---

## 9.3. Staff navigation

```text
Quét QR
Nhập mã vé
Lịch sử quét
Chi nhánh
```

Ưu tiên mobile.

---

# 10. Core Booking Flow

```text
Movie Discovery
      ↓
Movie Detail
      ↓
Select Cinema / Date / Showtime
      ↓
Authentication if required
      ↓
Seat Selection
      ↓
Seat Hold confirmed
      ↓
Checkout
      ↓
Create Payment
      ↓
External Payment Provider
      ↓
Payment Verification
      ├─ SUCCESS → Ticket
      ├─ PENDING → Verification state
      ├─ FAILED → Safe retry
      └─ LATE → Refund processing
```

Frontend không tự suy đoán trạng thái payment.

---

# 11. Customer Screen Specifications

# C01 — Home

## Objective

Giúp người dùng nhanh chóng:

- tìm phim;
- xem phim đang chiếu;
- xem phim sắp chiếu;
- bắt đầu booking.

## Desktop composition

```text
┌──────────────────────────────────────────────────────────────┐
│ Header                                                       │
├──────────────────────────────────────────────────────────────┤
│ Hero Featured Movie                                         │
│ title / metadata / description / CTA                poster   │
├──────────────────────────────────────────────────────────────┤
│ Quick Booking                                               │
│ Movie ▼ | Cinema ▼ | Date ▼ | Search                       │
├──────────────────────────────────────────────────────────────┤
│ Now Showing                                      View all → │
│ [Movie] [Movie] [Movie] [Movie]                             │
├──────────────────────────────────────────────────────────────┤
│ Coming Soon                                      View all → │
│ [Movie] [Movie] [Movie] [Movie]                             │
├──────────────────────────────────────────────────────────────┤
│ Cinemas                                                     │
├──────────────────────────────────────────────────────────────┤
│ Footer                                                      │
└──────────────────────────────────────────────────────────────┘
```

## Hero

- gradient sáng kết hợp ảnh phim;
- không full viewport;
- title;
- age rating;
- genre;
- duration;
- short description;
- CTA `Đặt vé`.

Không dùng carousel tự chạy nhanh.

## Movie Card

- poster `2:3`;
- title tối đa hai dòng;
- age rating;
- genre;
- duration;
- CTA `Đặt vé`.

Desktop:

```text
4–5 cards / row
```

Mobile:

```text
2 cards / row
```

## States

- loading → skeleton;
- no featured movie → bỏ hero, không để empty block;
- no movies → informative empty state;
- API failure → retry.

---

# C02 — Movie List

## Objective

Tìm phim đang chiếu hoặc sắp chiếu nhanh chóng.

## Content

- tab `Đang chiếu`;
- tab `Sắp chiếu`;
- search;
- genre filter;
- date filter;
- age rating filter.

Desktop filter:

```text
[Search________________] [Genre ▼] [Date ▼] [Age ▼]
```

Mobile:

```text
[Search________________] [Filter]
```

Filter mở drawer.

## UX rules

- filter state lưu trong URL;
- quay lại từ Movie Detail vẫn giữ filter;
- skeleton khi loading;
- empty state khi không có kết quả.

---

# C03 — Movie Detail

## Desktop composition

```text
┌──────────────────────────────────────────────────────────────┐
│ Breadcrumb                                                   │
├────────────────┬─────────────────────────────────────────────┤
│ Poster         │ Title                                       │
│                │ age / genre / duration                      │
│                │ release date                                │
│                │ description                                 │
│                │ [Watch trailer] [Choose showtime]           │
└────────────────┴─────────────────────────────────────────────┘

Showtimes
[Date tabs]
[Cinema groups]
```

Poster khoảng `300px`.

## Mobile

- poster;
- metadata;
- description;
- expandable description;
- CTA `Chọn suất chiếu` sticky bottom khi phù hợp.

## Content

- title;
- age rating;
- genre;
- duration;
- release date;
- director nếu có;
- cast nếu có;
- description;
- trailer;
- age restriction notice nếu cần.

---

# C04 — Showtime Selection

## Objective

Chọn suất chiếu phù hợp với ít thao tác nhất.

## Layout

```text
[Date 1] [Date 2] [Date 3] [Date 4] ...

Cinema A
10:30   13:20   16:10   20:00

Cinema B
09:45   12:15   18:30
```

Showtime Chip hiển thị:

- start time;
- screen format;
- price from.

## Rules

- group theo cinema;
- closed showtime vẫn readable nhưng disabled;
- low availability có badge nếu dữ liệu backend hỗ trợ;
- nhớ cinema được chọn gần nhất;
- đổi cinema không yêu cầu chọn lại movie.

---

# C05 — Seat Selection

## Objective

Giúp người dùng hiểu chính xác:

- ghế nào có thể chọn;
- ghế nào không thể chọn;
- ghế nào đang được giữ;
- thời gian hold còn lại;
- tổng tiền hiện tại.

Đây là màn hình UX có priority cao nhất.

## Desktop composition

```text
┌───────────────────────────────────────────────────────────────┐
│ Movie · Cinema · Room · Showtime                Hold 04:32   │
├──────────────────────────────────────┬────────────────────────┤
│                                      │ Booking Summary        │
│                SCREEN                │                        │
│                                      │ A5 · VIP    90.000 ₫  │
│      A1 A2 A3 A4 A5 A6 ...           │ A6 · VIP    90.000 ₫  │
│      B1 B2 B3 B4 B5 B6 ...           │                        │
│      C1 C2 C3 C4 C5 C6 ...           │ Total      180.000 ₫  │
│                                      │                        │
│      Seat Legend                     │ [ Continue ]           │
└──────────────────────────────────────┴────────────────────────┘
```

Seat map chiếm phần lớn viewport.

Booking Summary sticky bên phải.

## Mobile composition

```text
┌──────────────────────────────┐
│ ← Movie               04:32 │
├──────────────────────────────┤
│            SCREEN            │
│                              │
│       pan / pinch zoom       │
│                              │
│          seat map            │
│                              │
├──────────────────────────────┤
│ A5, A6        180.000 ₫      │
│          [ Continue ]        │
└──────────────────────────────┘
```

## Seat visual states

| State | UI |
|---|---|
| AVAILABLE | white + gray border |
| SELECTED | primary + check |
| HELD | light warning + clock |
| SOLD | gray + lock |
| BLOCKED | slash pattern/icon |
| VIP | badge/border |
| COUPLE | grouped/wide representation |

State không chỉ dựa vào color.

## Hold UX

- countdown chỉ bắt đầu khi backend xác nhận hold;
- countdown dùng server deadline;
- refresh không reset;
- chọn thêm ghế không kéo dài hold;
- ghế đôi toggle theo cặp;
- conflict chỉ rõ ghế bị conflict;
- WebSocket event kích hoạt REST sync, không thay thế source of truth.

## Failure states

### `409 Conflict`

```text
Một hoặc nhiều ghế vừa được người khác giữ.
```

Highlight ghế liên quan và đồng bộ lại seat map.

### `410 Gone`

```text
Thời gian giữ ghế đã hết.
```

Reset selection dựa trên trạng thái mới từ backend.

### Network failure

Hiển thị warning:

```text
Không thể cập nhật trạng thái ghế. Đang thử kết nối lại.
```

Không giả định seat state.

---

# C06 — Checkout

## Objective

Người dùng xác minh booking và khởi tạo thanh toán an toàn.

## Desktop

```text
┌───────────────────────────────────┬───────────────────────────┐
│ Customer                          │ Order Summary             │
│ Voucher                           │ Movie                     │
│ Payment Method                    │ Cinema                    │
│                                   │ Showtime                  │
│                                   │ Seats                     │
│                                   │ Subtotal                  │
│                                   │ Discount                  │
│                                   │ Fee                       │
│                                   │ Total                     │
│                                   │                           │
│                                   │ [Pay 180.000 ₫]           │
└───────────────────────────────────┴───────────────────────────┘
```

## Rules

- countdown luôn visible;
- không yêu cầu nhập lại dữ liệu đã có;
- giá lấy từ backend;
- hiển thị giá từng seat;
- hiển thị discount;
- hiển thị fee;
- CTA chứa total amount.

Example:

```text
Thanh toán 180.000 ₫
```

## Submit behavior

Sau lần click:

- disable CTA;
- show loading;
- không cho double submit.

Retry phải giữ idempotency key.

Nếu user rời trang khi hold/payment còn active, hiển thị confirmation khi phù hợp.

---

# C07 — Payment Result

Frontend luôn gọi backend để xác nhận trạng thái.

Redirect URL từ payment provider không phải bằng chứng payment success.

## State machine

### VERIFYING

Title:

```text
Đang xác nhận thanh toán
```

Body:

```text
Giao dịch đang được xác nhận. Vui lòng không thanh toán lại.
```

### SUCCESS

Hiển thị:

- success state;
- booking code;
- ticket summary;
- CTA `Xem vé`.

### FAILED

Hiển thị:

- friendly reason nếu có;
- không expose internal error;
- retry nếu booking vẫn valid.

### PAYMENT_REVIEW

```text
Giao dịch đang được đối soát.
```

Không khuyến khích thanh toán lại.

### REFUND_PENDING

```text
Thanh toán được ghi nhận sau thời hạn. Hệ thống đang xử lý hoàn tiền.
```

### EXPIRED

```text
Phiên đặt vé đã hết hạn và ghế đã được giải phóng.
```

---

# C08 — My Tickets

## Tabs

```text
Sắp xem
Đã xem
Đã hủy / hoàn
```

## Ticket card

Hiển thị:

- poster;
- title;
- datetime;
- cinema;
- auditorium;
- seats;
- booking code;
- status;
- CTA `Xem vé`.

Upcoming ticket có thể có:

```text
Yêu cầu hoàn vé
```

nếu backend xác định đủ điều kiện.

---

# C09 — Booking Detail

Hiển thị:

- booking code;
- booking status;
- movie;
- cinema;
- room;
- showtime;
- seat snapshot;
- price snapshot;
- payment status;
- refund status;
- ticket status.

Không hiển thị raw provider payload hoặc internal identifiers không cần thiết.

---

# C10 — Refund Request

## Flow

```text
Eligibility
→ Reason
→ Refund Preview
→ Confirmation
→ Processing
```

## Content

- refund conditions;
- refund amount;
- voucher restoration status;
- reason selection.

Final confirmation phải mô tả rõ tác động.

## Result states

```text
Đã tiếp nhận
→ Đang xử lý
→ Đã hoàn
```

hoặc:

```text
Đã tiếp nhận
→ Đang xử lý
→ Hoàn thất bại
```

Không dùng `Đã hoàn tiền` khi mới tạo request.

---

# C11 / C12 — Authentication

## Login

Fields:

- email;
- password.

Actions:

- login;
- forgot password;
- register.

## Register

Fields tối thiểu:

- full name;
- email;
- password;
- confirm password.

Validation message đặt gần input.

Không dùng toast làm phương thức duy nhất để báo validation error.

---

# 12. Staff Screen Specifications

# S01 — QR Scanner

Ưu tiên mobile/PWA.

## Composition

```text
┌────────────────────────────┐
│ LAK · Cinema A             │
├────────────────────────────┤
│                            │
│          CAMERA            │
│                            │
│                            │
├────────────────────────────┤
│ [Flash] [Nhập mã thủ công] │
└────────────────────────────┘
```

## Scan results

| Result | UX |
|---|---|
| VALID | success + movie + seat |
| USED | warning + previous scan time |
| WRONG_CINEMA | error + expected cinema |
| NOT_FOUND | error |
| EXPIRED | error / manager flow if applicable |

Không chỉ dùng âm thanh.

---

# S02 — Manual Ticket Lookup

Input:

```text
Ticket code / Booking code
```

Output:

- movie;
- showtime;
- seat;
- cinema;
- status;
- scan action nếu valid.

---

# S03 — Scan History

Danh sách:

- ticket code;
- time;
- scanner;
- result.

Filter theo result và time range nếu cần.

---

# 13. Admin UI Guidelines

Admin ưu tiên:

- data clarity;
- thao tác nhanh;
- mật độ vừa phải;
- confirmation rõ với destructive action.

## Data Table Pattern

```text
[Title]                            [Primary Action]

[Search________________] [Filter ▼] [Filter ▼]

┌───────────────────────────────────────────────────────┐
│ Column | Column | Status | ... | Actions             │
├───────────────────────────────────────────────────────┤
│ ...                                                   │
└───────────────────────────────────────────────────────┘

                    Pagination
```

Rules:

- sticky table header nếu bảng dài;
- filters phía trên;
- status dùng badge;
- row actions gom vào menu;
- không đặt nhiều button trực tiếp mỗi row;
- destructive actions cần confirmation.

---

# A01 — Dashboard

Widgets:

- revenue today;
- tickets sold;
- pending bookings;
- refunds needing attention;
- occupancy rate;
- revenue chart;
- top movies;
- top showtimes.

Không hiển thị chart nếu metric đơn giản có thể trình bày bằng KPI card.

---

# A02 — Movie Management

Table:

- poster;
- title;
- release date;
- duration;
- age rating;
- status;
- actions.

Actions:

- create;
- edit;
- manage genre;
- media;
- release status.

---

# A03 — Cinema Management

Table:

- cinema;
- city;
- status;
- number of auditoriums.

Cinema detail:

- address;
- auditorium list;
- operational status.

---

# A04 — Auditorium & Seat Layout

Seat layout editor phải:

- giữ row/seat label rõ;
- phân biệt seat type;
- hỗ trợ couple seat pair;
- cảnh báo thay đổi có thể ảnh hưởng dữ liệu liên quan.

Không cho UI tạo seat pair không hợp lệ.

---

# A05 — Showtime Management

Calendar/list view.

Fields:

- movie;
- auditorium;
- start time;
- end time;
- sales close time;
- status.

Conflict phải được trình bày rõ.

Không chỉ hiện lỗi generic.

---

# A06 — Pricing

Pricing UI phải cho thấy:

- price profile;
- effective time;
- seat type;
- screen format;
- day/time rule;
- priority;
- resulting price.

Khi có override showtime, UI phải hiển thị rõ đây là override.

---

# A07 — Booking Management

Search:

- booking code;
- email;
- phone.

Filters:

- status;
- cinema;
- date.

Booking detail chỉ cho thao tác nghiệp vụ được định nghĩa, không cho sửa transaction data trực tiếp.

---

# A08 — Payment Management

Hiển thị:

- booking;
- provider;
- amount;
- status;
- created time;
- paid time;
- transaction reference.

Không hiển thị secret hoặc raw signature.

---

# A09 — Refund Management

Statuses:

- REQUESTED;
- PROCESSING;
- REFUNDED;
- FAILED;
- MANUAL_REVIEW nếu backend có.

Các action thủ công cần confirmation và audit expectation.

---

# A10 — Voucher Management

Hiển thị:

- code;
- discount;
- date range;
- usage;
- per-user limit;
- status.

---

# A11 — User Management

Hiển thị:

- name;
- email;
- role;
- account status;
- cinema scope nếu staff;
- actions.

Lock/unlock phải có confirmation.

---

# A12 — Reports

Core report dimensions:

- time;
- movie;
- cinema.

Core metrics:

- revenue;
- tickets sold;
- bookings;
- occupancy.

Timezone hiển thị theo `Asia/Ho_Chi_Minh`.

---

# 14. Component Contracts

# 14.1. Button

Variants:

```text
Primary
Secondary
Outline
Ghost
Danger
```

States:

```text
Default
Hover
Focus
Active
Disabled
Loading
```

Minimum height:

```text
44px
```

Loading không làm thay đổi width đột ngột.

---

# 14.2. Input

States:

- default;
- focus;
- disabled;
- error;
- success nếu cần.

Mỗi input phải có label.

Error message đặt gần field.

---

# 14.3. MovieCard

## Content

- poster;
- age rating;
- title;
- genre;
- duration;
- CTA.

## States

- loading;
- default;
- hover;
- unavailable nếu applicable.

Poster ratio:

```text
2:3
```

---

# 14.4. ShowtimeChip

## Content

- start time;
- screen format;
- starting price.

## States

- available;
- selected;
- low availability;
- closed.

Closed:

- visually muted;
- disabled;
- vẫn readable.

---

# 14.5. Seat

## Data contract

```text
seatNumber
row
seatType
state
selected
disabled
pairId
```

## Seat types

```text
STANDARD
VIP
COUPLE
```

## Business states

```text
AVAILABLE
HELD
SOLD
BLOCKED
```

## Accessibility

Example:

```text
Ghế A5, loại VIP, còn trống
```

Minimum interactive target:

```text
44 × 44px
```

Couple seat phải toggle cùng cặp.

---

# 14.6. SeatGroup

Responsibilities:

- maintain row alignment;
- show row labels;
- show walkway spacing;
- group couple seats;
- support zoom/pan on mobile.

Không tự quyết định seat business state.

---

# 14.7. BookingSummary

## Content

- movie;
- cinema;
- auditorium;
- showtime;
- seats;
- subtotal;
- discount;
- fee;
- total;
- countdown.

Desktop:

```text
sticky sidebar
```

Mobile:

```text
bottom sheet / sticky bottom summary
```

---

# 14.8. Countdown

Props conceptually:

```text
deadline
serverNow
warningThreshold
```

Không lấy refresh time làm deadline mới.

States:

- normal;
- warning;
- expired.

---

# 14.9. StatusBadge

Badge luôn có text.

Không chỉ dùng màu.

Examples:

```text
Đã thanh toán
Chờ thanh toán
Đang hoàn tiền
Đã sử dụng
```

---

# 14.10. QR Ticket

Hiển thị:

- QR;
- ticket code;
- movie;
- time;
- cinema;
- auditorium;
- seats;
- ticket status.

QR phải có:

- nền trắng;
- đủ quiet zone;
- kích thước dễ scan.

---

# 14.11. Modal / Confirmation Dialog

Modal:

- focus trap;
- Esc đóng được nếu không phải critical non-dismissible state;
- return focus về trigger sau khi đóng.

Destructive dialog phải có:

- action;
- consequence;
- explicit confirmation.

---

# 14.12. Toast

Toast dùng cho:

- confirmation ngắn;
- non-blocking feedback.

Không dùng toast làm nơi duy nhất hiển thị:

- validation;
- critical payment state;
- critical booking error.

---

# 14.13. Skeleton

Skeleton phải giữ gần đúng layout cuối.

Không gây layout shift lớn.

---

# 14.14. Empty State

Bao gồm:

- title;
- explanation;
- CTA nếu có next action.

Ví dụ:

```text
Bạn chưa có vé sắp xem.
```

---

# 14.15. Error State

Bao gồm:

- user-friendly explanation;
- retry nếu safe;
- navigation fallback nếu cần.

Không expose:

- stack trace;
- SQL error;
- internal exception;
- provider secret;
- internal implementation detail.

---

# 15. Global State Matrix

Mỗi màn P0 phải đánh giá các trạng thái sau.

| State | Required |
|---|---|
| Initial | Yes |
| Loading | Yes |
| Success | Yes |
| Empty | When applicable |
| Validation Error | When applicable |
| API Error | Yes |
| Network Failure | Yes |
| Unauthorized | Protected screens |
| Forbidden | Role-scoped screens |
| Conflict `409` | Booking flow |
| Expired `410` | Hold/payment flow |
| Retry | Where operation is retry-safe |

---

# 16. Error Mapping

Frontend nên map lỗi theo semantic behavior.

| HTTP | UX |
|---|---|
| `400` | Request invalid / generic field error |
| `401` | Re-authenticate |
| `403` | Không đủ quyền |
| `409` | Conflict state |
| `410` | Resource/hold expired |
| `422` | Business validation error |
| `429` | Rate limited |

Không hiển thị HTTP code cho người dùng thông thường nếu không cần thiết.

---

# 17. Realtime UX

WebSocket chỉ dùng để thông báo thay đổi.

Pattern:

```text
WebSocket event
→ mark data stale
→ refetch authoritative REST resource
→ render new state
```

Nếu WebSocket disconnect:

- hiển thị warning nếu ảnh hưởng screen hiện tại;
- vẫn sử dụng REST;
- reconnect background.

Không khóa toàn bộ app vì mất WebSocket.

---

# 18. Responsive Transformation Rules

| Desktop | Mobile |
|---|---|
| Full header navigation | Compact navigation |
| Horizontal filter row | Filter drawer |
| Booking sidebar | Bottom sheet |
| Movie detail 2 columns | Stacked |
| Seat map fixed area | Pan + zoom |
| Admin table | Horizontal scroll / card when necessary |
| Inline CTA | Sticky bottom CTA when appropriate |

Không ẩn:

- price;
- booking status;
- hold countdown;
- payment state;

chỉ vì màn hình nhỏ.

---

# 19. Accessibility

## Required

- touch target minimum `44 × 44px`;
- normal text contrast ≥ `4.5:1`;
- visible focus ring;
- keyboard navigation;
- input labels;
- field-level errors;
- alt text;
- modal focus trap;
- Esc support;
- status not color-only;
- seat screen reader label;
- reduced motion support.

Suggested focus:

```css
outline: 2px solid #2563EB;
outline-offset: 2px;
```

---

# 20. Motion

| Element | Motion |
|---|---|
| Button | color transition `150ms` |
| Movie card | lift `2px` + light shadow |
| Seat selection | small scale `120ms` |
| Modal | fade + scale `200ms` |
| Drawer | slide `200ms` |
| Toast | short slide `200ms` |
| Loading | subtle skeleton |

Không dùng:

- strong parallax;
- complex 3D;
- fast auto carousel;
- animation everywhere;
- heavy shadow;
- excessive gradient.

Hỗ trợ:

```css
@media (prefers-reduced-motion: reduce)
```

---

# 21. Loading Strategy

## Page-level

Ưu tiên skeleton nếu cấu trúc nội dung đã biết.

## Button-level

Button loading:

- giữ width;
- disable repeated action;
- có loading indicator;
- label vẫn có semantic meaning nếu cần.

## Payment

Payment verification sử dụng dedicated state, không dùng generic spinner vô thời hạn.

---

# 22. Empty State Strategy

Examples:

## Movie List

```text
Không tìm thấy phim phù hợp với bộ lọc.
```

Action:

```text
Xóa bộ lọc
```

## My Tickets

```text
Bạn chưa có vé sắp xem.
```

Action:

```text
Khám phá phim
```

## Admin Search

```text
Không có dữ liệu khớp với điều kiện tìm kiếm.
```

---

# 23. Confirmation Strategy

Bắt buộc confirmation đối với:

- cancel showtime;
- price change có ảnh hưởng;
- lock account;
- refund;
- destructive cinema/seat operation;
- leaving active booking flow nếu có nguy cơ mất hold.

Confirmation phải nói rõ hậu quả.

Không dùng:

```text
Bạn có chắc không?
```

mà không có context.

---

# 24. Content & Microcopy Guidelines

## Voice

- rõ;
- ngắn;
- trung tính;
- không technical.

## Good

```text
Ghế A5 vừa được người khác giữ. Vui lòng chọn ghế khác.
```

## Avoid

```text
409 CONFLICT — showtimeSeat unavailable.
```

## Good

```text
Giao dịch đang được xác nhận. Vui lòng không thanh toán lại.
```

## Avoid

```text
Webhook chưa về.
```

---

# 25. Currency & Time Presentation

## Currency

VND hiển thị:

```text
180.000 ₫
```

Không hiển thị decimal không cần thiết.

## Time

Backend truyền thời gian UTC.

UI hiển thị theo:

```text
Asia/Ho_Chi_Minh
```

Showtime nên hiển thị:

```text
20:30
Thứ Sáu, 18/09
```

khi cần context ngày.

---

# 26. Search & Filter Guidelines

- filter state dùng URL khi có lợi;
- back/forward browser phải hoạt động hợp lý;
- clear-all filters dễ tìm;
- search debounce khi phù hợp;
- không search lại chỉ vì UI state không liên quan thay đổi.

---

# 27. Form Validation

Validation hierarchy:

```text
1. Client format validation
2. Backend business validation
3. Server state validation
```

Frontend không duplicate business rule phức tạp làm source of truth.

Field error đặt ngay dưới field.

Form-level error dùng khi lỗi không thuộc một field cụ thể.

---

# 28. Security-sensitive UX

Không hiển thị:

- raw access token;
- refresh token;
- QR raw secret outside intended QR rendering;
- payment secret;
- webhook payload;
- stack trace;
- internal SQL identifiers.

Các màn role-based không chỉ ẩn menu; backend vẫn phải enforce permission.

---

# 29. UX Priority

## Level 1 — Core Booking

- Home;
- Movie List;
- Movie Detail;
- Showtime Selection;
- Seat Selection;
- Checkout;
- Payment Result;
- My Tickets;
- Login/Register.

## Level 2 — Operations

- Admin Dashboard;
- Movie Management;
- Cinema/Room Management;
- Showtime Management;
- Booking/Payment/Refund Management;
- Staff Scanner.

## Level 3 — Refinement

- advanced tablet optimization;
- usability testing;
- refined micro-interactions;
- visual polish;
- additional reporting UX.

Loading, error, responsive và accessibility **không thuộc Level 3**. Chúng phải được xử lý cùng screen tương ứng.

---

# 30. UX Definition of Done

Một screen chỉ được xem là hoàn tất khi:

```text
[ ] Đúng screen specification.
[ ] Đúng design tokens.
[ ] Đúng navigation và role.
[ ] Desktop responsive.
[ ] Mobile responsive.
[ ] Loading state.
[ ] Empty state nếu applicable.
[ ] Error state.
[ ] Network failure handling.
[ ] Retry behavior an toàn.
[ ] Validation rõ.
[ ] Keyboard usable.
[ ] Focus visible.
[ ] Accessibility labels.
[ ] Không dùng màu làm tín hiệu duy nhất.
[ ] Business state khớp backend.
[ ] Không tự tính authoritative data ở frontend.
[ ] Không tạo duplicate action khi retry.
[ ] Không expose dữ liệu nhạy cảm.
[ ] Không có layout shift đáng kể khi loading.
```

---

# 31. Coding Agent Rules

Khi coding agent triển khai frontend, phải tuân thủ:

1. Không tự thêm business state ngoài contract backend.
2. Không tự tính authoritative ticket price.
3. Không reset countdown khi refresh.
4. Không coi WebSocket là source of truth.
5. Không coi payment redirect là payment success.
6. Không tạo duplicate booking/payment khi retry.
7. Không bỏ loading/error/empty states.
8. Không chỉ triển khai desktop.
9. Không bỏ keyboard/focus/accessibility.
10. Không tạo visual pattern mới nếu component đã tồn tại.
11. Reuse design tokens và component primitives.
12. Mọi destructive admin action phải có confirmation.
13. Mọi protected screen phải xử lý unauthorized/forbidden.
14. Không expose raw technical error ra UI.

---

# 32. Stakeholder Review Checklist

Stakeholder nên review từng màn theo các tiêu chí:

## Content

- thông tin nào cần xuất hiện?
- có thiếu dữ liệu quan trọng không?

## Flow

- người dùng có biết bước tiếp theo không?
- có bước nào dư không?

## Trust

- giá có rõ không?
- booking/payment status có rõ không?
- error có gây hiểu sai không?

## Visual

- hierarchy có rõ không?
- CTA chính có nổi bật đúng mức không?
- có quá nhiều thông tin không?

## Mobile

- thao tác có dễ bằng một tay không?
- CTA có dễ bấm không?
- critical state có luôn visible không?

---

# 33. Các màn hình cần usability test ưu tiên

## 1. Seat Selection

Rủi ro:

- hiểu sai trạng thái ghế;
- conflict;
- countdown;
- couple seat;
- mobile pan/zoom.

## 2. Checkout

Rủi ro:

- hiểu sai tổng tiền;
- voucher;
- countdown;
- accidental duplicate payment.

## 3. Payment Result

Rủi ro:

- user thanh toán lại khi payment pending;
- hiểu nhầm late payment;
- hiểu nhầm refund status.

---

# 34. Deliverables UI/UX tiếp theo

Sau tài liệu này, nên duy trì thêm:

```text
docs/
├── overview.md
├── BE/
│   └── design-systems.md
└── FE/
    ├── ui-ux.md
    ├── wireframes.md
    └── ui-flows.md
```

## `wireframes.md`

Ưu tiên wireframe:

```text
C01 Home
C02 Movie List
C03 Movie Detail
C04 Showtime Selection
C05 Seat Selection
C06 Checkout
C07 Payment Result
C08 My Tickets
S01 QR Scanner
A01 Dashboard
A05 Showtime Management
A07 Booking Management
```

## `ui-flows.md`

Chỉ cần tách riêng nếu flow/state diagram trở nên đủ phức tạp.

Các flow nên mô tả:

- booking;
- seat conflict;
- payment;
- late payment;
- refund;
- ticket scan.

---

# 35. Kết luận

LAK sử dụng một design system chung nhưng có ba phong cách trình bày theo ngữ cảnh:

1. **Customer:** thoáng, trực quan, giàu hình ảnh và ưu tiên conversion.
2. **Admin:** rõ dữ liệu, thao tác nhanh, mật độ thông tin vừa phải.
3. **Staff:** tối giản, tập trung vào tốc độ và phản hồi tức thời.

UI phải luôn phản ánh đúng trạng thái nghiệp vụ của backend.

Ba nguyên tắc quan trọng nhất của toàn bộ trải nghiệm là:

```text
State clarity
Price transparency
Failure-safe booking
```

Một UI đẹp nhưng làm người dùng hiểu sai trạng thái ghế hoặc payment được xem là không đạt yêu cầu.

Một UI đúng nghiệp vụ nhưng khó sử dụng trên mobile hoặc không xử lý loading/error/accessibility cũng được xem là chưa hoàn tất.

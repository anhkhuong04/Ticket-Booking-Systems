# LAK — Wireframes Specification

> **Phiên bản:** 1.0  
> **Phạm vi:** Customer website, Staff scanner và Admin console  
> **Tài liệu liên quan:** `docs/overview.md`, `docs/BE/design-systems.md`, `docs/FE/ui-ux.md`, `task.md`
> **Mục tiêu:** Cung cấp layout contract đủ rõ để stakeholder hình dung sản phẩm và coding agent triển khai UI mà không phải tự suy diễn bố cục chính.

---

# 1. Mục đích tài liệu

`wireframes.md` mô tả **cấu trúc không gian và thứ tự nội dung** của các màn hình LAK.

Tài liệu này tập trung vào:

- information hierarchy;
- vị trí các vùng nội dung;
- CTA chính;
- navigation;
- responsive transformation;
- loading / empty / error / conflict state;
- relationship giữa các component.

Tài liệu này **không phải visual mockup pixel-perfect**.

Các chi tiết sau phải lấy từ `docs/FE/ui-ux.md`:

- màu sắc;
- typography;
- spacing;
- radius;
- shadow;
- motion;
- accessibility;
- component state;
- content guideline.

Các trạng thái nghiệp vụ phải lấy từ backend/system design tại `docs/BE/design-systems.md`.

---

# 2. Nguyên tắc sử dụng wireframe

## 2.1. Wireframe là layout contract

Coding agent được phép điều chỉnh:

- khoảng cách nhỏ;
- cách wrap text;
- kích thước responsive chi tiết;
- alignment vi mô;

nhưng không được tự ý thay đổi:

- hierarchy;
- CTA chính;
- thứ tự block;
- vị trí critical state;
- luồng navigation;
- booking/payment semantics.

## 2.2. Backend là source of truth

Wireframe không quyết định:

- seat availability;
- price;
- hold deadline;
- booking status;
- payment status;
- refund status;
- ticket validity.

Frontend chỉ render state nhận từ backend.

## 2.3. Mobile không phải desktop thu nhỏ

Khi chuyển xuống mobile:

- sidebar có thể thành bottom sheet;
- filter row có thể thành drawer;
- table có thể thành horizontal scroll hoặc card list;
- CTA quan trọng có thể sticky bottom;
- seat map phải hỗ trợ pan/zoom.

Không được ẩn critical information chỉ để tiết kiệm diện tích.

---

# 3. Ký hiệu wireframe

```text
[Button]                 CTA / button
[Input________________]  input
[Select ▼]               select
[Tab]                    tab
[Chip]                   chip
[Card]                   card
[Badge]                  badge
[...]                     content repeated
▼                         dropdown
← →                       navigation
```

Container:

```text
┌──────────────┐
│              │
└──────────────┘
```

Hai cột:

```text
┌──────────────────────┬───────────────┐
│ Main                 │ Sidebar       │
└──────────────────────┴───────────────┘
```

---

# 4. Global Shell

## 4.1. Customer Desktop Shell

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ LAK      Phim      Rạp      [Tìm kiếm...]       Vé của tôi      Tài khoản │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│                         PAGE CONTENT                                       │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│ Footer                                                                     │
└────────────────────────────────────────────────────────────────────────────┘
```

### Rules

- Header sticky nhẹ.
- Container tối đa `1200–1280px`.
- Chi nhánh hiện tại có thể hiển thị gần logo/search nếu context yêu cầu.
- Footer không xuất hiện trong booking flow nếu làm giảm không gian cần thiết trên mobile.

## 4.2. Customer Mobile Shell

```text
┌──────────────────────────────┐
│ LAK        Search   Ticket ☰ │
├──────────────────────────────┤
│                              │
│         PAGE CONTENT         │
│                              │
├──────────────────────────────┤
│ optional sticky action       │
└──────────────────────────────┘
```

## 4.3. Admin Desktop Shell

```text
┌──────────────────┬───────────────────────────────────────────────────────┐
│ LAK ADMIN        │ Topbar                                                │
│                  ├───────────────────────────────────────────────────────┤
│ Dashboard        │                                                       │
│ Phim             │                                                       │
│ Rạp & phòng      │                  PAGE CONTENT                         │
│ Suất chiếu       │                                                       │
│ Bảng giá         │                                                       │
│ Booking          │                                                       │
│ Thanh toán       │                                                       │
│ Hoàn tiền        │                                                       │
│ Voucher          │                                                       │
│ Người dùng       │                                                       │
│ Báo cáo          │                                                       │
│                  │                                                       │
└──────────────────┴───────────────────────────────────────────────────────┘
```

Sidebar khoảng `240px`.

## 4.4. Staff Mobile Shell

```text
┌──────────────────────────────┐
│ LAK Staff       Cinema A     │
├──────────────────────────────┤
│                              │
│         PAGE CONTENT         │
│                              │
├──────────────────────────────┤
│ Scanner | Lookup | History   │
└──────────────────────────────┘
```

---

# 5. Customer Flow Overview

```text
C01 Home
   ↓
C02 Movie List
   ↓
C03 Movie Detail
   ↓
C04 Showtime Selection
   ↓
C05 Seat Selection
   ↓
C06 Checkout
   ↓
External Payment
   ↓
C07 Payment Result
   ↓
C08 My Tickets
   ↓
C09 Booking Detail
   ↓
C10 Refund Request
```

Auth có thể xen vào trước bước protected:

```text
C11 Login
C12 Register
C13 Forgot Password
C14 Reset Password
```

---

# 6. C01 — Home

## 6.1. Mục tiêu

- Giúp user biết phim nổi bật.
- Bắt đầu booking nhanh.
- Khám phá phim đang chiếu / sắp chiếu.
- Xem các chi nhánh.

## 6.2. Desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  HERO FEATURED MOVIE                                      Featured Poster  │
│                                                                            │
│  MOVIE TITLE                                                ┌───────────┐  │
│  T16 · Action · 125 phút                                    │           │  │
│  Short description...                                       │  POSTER   │  │
│                                                            │           │  │
│  [Đặt vé]   [Xem trailer]                                   └───────────┘  │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  ĐẶT VÉ NHANH                                                              │
│                                                                            │
│  [Chọn phim ▼] [Chọn rạp ▼] [Chọn ngày ▼]                 [Tìm suất chiếu]│
├────────────────────────────────────────────────────────────────────────────┤
│  PHIM ĐANG CHIẾU                                             Xem tất cả → │
│                                                                            │
│  [ Movie ]   [ Movie ]   [ Movie ]   [ Movie ]   [ Movie ]                │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  PHIM SẮP CHIẾU                                              Xem tất cả → │
│                                                                            │
│  [ Movie ]   [ Movie ]   [ Movie ]   [ Movie ]   [ Movie ]                │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  RẠP LAK                                                                   │
│                                                                            │
│  [ Cinema Card ]   [ Cinema Card ]   [ Cinema Card ]                       │
├────────────────────────────────────────────────────────────────────────────┤
│ Footer                                                                     │
└────────────────────────────────────────────────────────────────────────────┘
```

## 6.3. Mobile

```text
┌──────────────────────────────┐
│ LAK        Search   Ticket ☰ │
├──────────────────────────────┤
│ FEATURED                     │
│                              │
│ [Backdrop / Poster]          │
│                              │
│ MOVIE TITLE                  │
│ T16 · Action · 125 phút      │
│                              │
│ [Đặt vé]                     │
├──────────────────────────────┤
│ Đặt vé nhanh                 │
│                              │
│ [Chọn phim ▼]                │
│ [Chọn rạp ▼]                 │
│ [Chọn ngày ▼]                │
│ [Tìm suất chiếu]             │
├──────────────────────────────┤
│ Phim đang chiếu    Xem tất cả│
│                              │
│ [Movie] [Movie]              │
│ [Movie] [Movie]              │
├──────────────────────────────┤
│ Phim sắp chiếu     Xem tất cả│
│                              │
│ [Movie] [Movie]              │
├──────────────────────────────┤
│ Rạp LAK                      │
│ [Cinema Card]                │
│ [Cinema Card]                │
└──────────────────────────────┘
```

## 6.4. States

### Loading

- Hero skeleton.
- Quick booking shell giữ nguyên.
- Movie card skeleton.

### Empty featured movie

Bỏ hero, không render block rỗng.

### Movie API error

```text
Không thể tải danh sách phim.
[Thử lại]
```

---

# 7. C02 — Movie List

## 7.1. Desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├────────────────────────────────────────────────────────────────────────────┤
│ PHIM                                                                       │
│                                                                            │
│ [Đang chiếu] [Sắp chiếu]                                                   │
│                                                                            │
│ [Tìm tên phim____________________] [Thể loại ▼] [Ngày ▼] [Độ tuổi ▼]      │
│                                                                            │
│  24 phim                                                                   │
│                                                                            │
│ [Movie] [Movie] [Movie] [Movie]                                            │
│ [Movie] [Movie] [Movie] [Movie]                                            │
│ [Movie] [Movie] [Movie] [Movie]                                            │
│                                                                            │
│                           [Pagination]                                      │
└────────────────────────────────────────────────────────────────────────────┘
```

## 7.2. Mobile

```text
┌──────────────────────────────┐
│ ← Phim                       │
├──────────────────────────────┤
│ [Đang chiếu] [Sắp chiếu]     │
│                              │
│ [Tìm tên phim_____________]  │
│ [Bộ lọc 3]                   │
│                              │
│ [Movie] [Movie]              │
│ [Movie] [Movie]              │
│ [Movie] [Movie]              │
└──────────────────────────────┘
```

Filter drawer:

```text
┌──────────────────────────────┐
│ Bộ lọc                  [X]  │
├──────────────────────────────┤
│ Thể loại                    │
│ [Action ▼]                  │
│                             │
│ Ngày                        │
│ [15/09/2026 ▼]              │
│                             │
│ Phân loại tuổi              │
│ [Tất cả ▼]                  │
│                             │
│ [Xóa bộ lọc] [Áp dụng]      │
└──────────────────────────────┘
```

## 7.3. Empty

```text
Không tìm thấy phim phù hợp.
Hãy thử thay đổi bộ lọc.

[Xóa bộ lọc]
```

---

# 8. C03 — Movie Detail

## 8.1. Desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├────────────────────────────────────────────────────────────────────────────┤
│ Trang chủ / Phim / Movie Title                                             │
│                                                                            │
│ ┌───────────────┐   MOVIE TITLE                                            │
│ │               │                                                         │
│ │               │   [T16] Action · Adventure · 125 phút                   │
│ │    POSTER     │   Khởi chiếu 18/09/2026                                 │
│ │               │                                                         │
│ │               │   Mô tả phim...                                         │
│ │               │                                                         │
│ └───────────────┘   Đạo diễn: ...                                         │
│                     Diễn viên: ...                                         │
│                                                                            │
│                     [Chọn suất chiếu] [Xem trailer]                         │
├────────────────────────────────────────────────────────────────────────────┤
│ SUẤT CHIẾU                                                                 │
│                                                                            │
│ [15/09] [16/09] [17/09] [18/09] [19/09]                                   │
│                                                                            │
│ LAK Cinema A                                                               │
│ [10:30] [13:20] [16:10] [20:00]                                           │
│                                                                            │
│ LAK Cinema B                                                               │
│ [09:45] [12:15] [18:30]                                                    │
└────────────────────────────────────────────────────────────────────────────┘
```

## 8.2. Mobile

```text
┌──────────────────────────────┐
│ ← Movie Detail               │
├──────────────────────────────┤
│       [Poster]               │
│                              │
│ MOVIE TITLE                  │
│ [T16] Action · 125 phút      │
│ 18/09/2026                   │
│                              │
│ Mô tả phim...                │
│ [Xem thêm]                   │
│                              │
│ [Xem trailer]                │
├──────────────────────────────┤
│ Suất chiếu                   │
│ [15] [16] [17] [18] →       │
│                              │
│ LAK Cinema A                 │
│ [10:30] [13:20] [16:10]     │
├──────────────────────────────┤
│ [Chọn suất chiếu]            │
└──────────────────────────────┘
```

CTA sticky bottom khi user chưa cuộn tới showtime block hoặc khi flow yêu cầu.

---

# 9. C04 — Showtime Selection

## 9.1. Desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├────────────────────────────────────────────────────────────────────────────┤
│ ← Movie Title                                                              │
│                                                                            │
│ CHỌN SUẤT CHIẾU                                                            │
│                                                                            │
│ [15 Thứ 3] [16 Thứ 4] [17 Thứ 5] [18 Thứ 6] [19 Thứ 7]                  │
│                                                                            │
│ [Chi nhánh hiện tại ▼]                                                     │
│                                                                            │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ LAK Cinema A                                           123 Address     │ │
│ │                                                                        │ │
│ │ 2D       [10:30] [13:20] [16:10] [20:00]                              │ │
│ │ IMAX     [12:00] [18:00]                                               │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ LAK Cinema B                                                           │ │
│ │ 2D       [09:45] [12:15] [18:30]                                      │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────┘
```

## 9.2. Mobile

```text
┌──────────────────────────────┐
│ ← Chọn suất                  │
├──────────────────────────────┤
│ Movie Title                  │
│ [15] [16] [17] [18] →       │
│                              │
│ [Chi nhánh ▼]                │
│                              │
│ LAK Cinema A                 │
│ 123 Address                  │
│                              │
│ 2D                           │
│ [10:30] [13:20] [16:10]     │
│ [20:00]                      │
│                              │
│ IMAX                         │
│ [12:00] [18:00]             │
├──────────────────────────────┤
│ LAK Cinema B                 │
│ ...                          │
└──────────────────────────────┘
```

## 9.3. Showtime Chip

```text
┌──────────┐
│ 20:30    │
│ từ 90k   │
└──────────┘
```

Closed:

```text
┌──────────┐
│ 20:30    │
│ Đã đóng  │
└──────────┘
```

---

# 10. C05 — Seat Selection

## 10.1. Mục tiêu

Màn hình phải ưu tiên:

1. seat availability;
2. selected seats;
3. hold countdown;
4. total;
5. CTA.

Không để poster hoặc decoration chiếm không gian đáng kể.

## 10.2. Desktop

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│ ← Movie Title             LAK Cinema A · Room 2      20:30 · 18/09          │
│                                                              Giữ ghế 04:32 │
├───────────────────────────────────────────────────┬──────────────────────────┤
│                                                   │ TÓM TẮT ĐẶT VÉ           │
│                  MÀN HÌNH                         │                          │
│            ═══════════════════                    │ Movie Title              │
│                                                   │ Cinema A · Room 2        │
│                                                   │ 20:30 · 18/09            │
│         A   □ □ □ □   □ □ □ □                    │                          │
│         B   □ □ □ □   □ □ □ □                    │ Ghế                      │
│         C   □ □ ■ ■   □ □ □ □                    │ A5 · VIP       90.000 ₫ │
│         D   □ □ □ □   ▣ ▣ □ □                    │ A6 · VIP       90.000 ₫ │
│         E   ═══ ═══   ═══ ═══                    │                          │
│                                                   │ Tạm tính      180.000 ₫ │
│                                                   │ Tổng          180.000 ₫ │
│                                                   │                          │
│  □ Trống   ■ Đang chọn   ◷ Đang giữ              │ [Tiếp tục]               │
│  ▣ Đã bán  ╱ Bị khóa     ═ Ghế đôi               │                          │
│                                                   │                          │
└───────────────────────────────────────────────────┴──────────────────────────┘
```

### Layout ratio

Main seat area:

```text
~70%
```

Summary:

```text
~30%
```

Summary sticky trong viewport.

## 10.3. Mobile

```text
┌──────────────────────────────┐
│ ← Chọn ghế             04:32 │
│ Movie · 20:30                │
├──────────────────────────────┤
│                              │
│         MÀN HÌNH             │
│      ═════════════           │
│                              │
│    ← pan / pinch zoom →      │
│                              │
│ A   □ □ □ □   □ □ □ □       │
│ B   □ □ ■ ■   □ □ □ □       │
│ C   □ □ □ □   ▣ ▣ □ □       │
│ D   ═══ ═══   ═══ ═══       │
│                              │
│ [−]                   [+]    │
├──────────────────────────────┤
│ □ Trống  ■ Chọn  ◷ Giữ      │
├──────────────────────────────┤
│ A5, A6        180.000 ₫      │
│ [Tiếp tục]                   │
└──────────────────────────────┘
```

Bottom area sticky.

Tap summary có thể mở bottom sheet:

```text
┌──────────────────────────────┐
│ Tóm tắt đặt vé          [X]  │
├──────────────────────────────┤
│ Movie Title                  │
│ Cinema A · Room 2            │
│ 20:30 · 18/09                │
│                              │
│ A5 VIP            90.000 ₫   │
│ A6 VIP            90.000 ₫   │
│                              │
│ Tổng             180.000 ₫   │
│                              │
│ [Tiếp tục]                   │
└──────────────────────────────┘
```

## 10.4. Loading

```text
Header metadata loaded

            MÀN HÌNH
        ═══════════════

        [seat skeleton]
        [seat skeleton]
        [seat skeleton]

Summary skeleton
```

Không render seat giả có thể click.

## 10.5. Conflict `409`

Inline state:

```text
┌─────────────────────────────────────────┐
│ ⚠ Ghế A5 vừa được người khác giữ.      │
│ Sơ đồ ghế đã được cập nhật.             │
└─────────────────────────────────────────┘
```

Ghế conflict highlight tạm thời rồi render state authoritative mới.

## 10.6. Hold Expired `410`

Modal:

```text
┌──────────────────────────────────────┐
│ Thời gian giữ ghế đã hết            │
│                                      │
│ Ghế của bạn đã được giải phóng.      │
│ Vui lòng chọn lại ghế.               │
│                                      │
│                         [Chọn lại]   │
└──────────────────────────────────────┘
```

Không giữ selected UI cũ sau khi backend trả trạng thái mới.

## 10.7. WebSocket disconnected

Banner nhỏ, không blocking:

```text
⚠ Kết nối realtime bị gián đoạn. Trạng thái ghế đang được đồng bộ lại.
```

---

# 11. C06 — Checkout

## 11.1. Desktop

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│ ← Chọn ghế                    CHECKOUT                      Giữ ghế 03:41    │
├───────────────────────────────────────────┬──────────────────────────────────┤
│ THÔNG TIN KHÁCH HÀNG                     │ TÓM TẮT ĐƠN HÀNG                 │
│                                           │                                  │
│ Họ tên                                    │ Movie Title                      │
│ [Nguyễn Văn A_________________________]   │ Cinema A · Room 2                │
│                                           │ 20:30 · 18/09                    │
│ Email                                     │                                  │
│ [adam@example.com_____________________]   │ Ghế                              │
│                                           │ A5 VIP              90.000 ₫    │
│ VOUCHER                                   │ A6 VIP              90.000 ₫    │
│ [Nhập mã________________] [Áp dụng]       │                                  │
│                                           │ Tạm tính           180.000 ₫    │
│ PHƯƠNG THỨC THANH TOÁN                    │ Giảm giá                  0 ₫    │
│                                           │ Phí dịch vụ               0 ₫    │
│ (•) VNPay                                 │ ───────────────────────────────  │
│ ( ) MoMo                                  │ Tổng               180.000 ₫    │
│                                           │                                  │
│                                           │ [Thanh toán 180.000 ₫]           │
└───────────────────────────────────────────┴──────────────────────────────────┘
```

## 11.2. Mobile

```text
┌──────────────────────────────┐
│ ← Checkout             03:41 │
├──────────────────────────────┤
│ Thông tin đơn                │
│ Movie Title                  │
│ Cinema A · Room 2            │
│ 20:30 · 18/09                │
│ A5, A6                       │
│                              │
│ [Xem chi tiết giá]           │
├──────────────────────────────┤
│ Thông tin khách hàng         │
│ [Họ tên___________________]  │
│ [Email____________________]  │
├──────────────────────────────┤
│ Voucher                      │
│ [Mã___________] [Áp dụng]    │
├──────────────────────────────┤
│ Thanh toán                   │
│ (•) VNPay                    │
│ ( ) MoMo                     │
├──────────────────────────────┤
│ Tổng             180.000 ₫   │
│ [Thanh toán 180.000 ₫]       │
└──────────────────────────────┘
```

Bottom total/action sticky nếu nội dung dài.

## 11.3. Submit Loading

```text
[Đang tạo giao dịch...]
```

Button:

- disabled;
- giữ width;
- không cho double click.

## 11.4. Voucher invalid

Field-level:

```text
[SUMMER2026_____________] [Áp dụng]
Mã giảm giá không hợp lệ hoặc đã hết hạn.
```

Không chỉ toast.

---

# 12. C07 — Payment Result

## 12.1. VERIFYING

Desktop / mobile đều dùng centered status card.

```text
┌──────────────────────────────────────┐
│                ◌                     │
│                                      │
│      Đang xác nhận thanh toán        │
│                                      │
│ Giao dịch đang được xác nhận.        │
│ Vui lòng không thanh toán lại.       │
│                                      │
│ Booking: LAK-AB12CD                  │
└──────────────────────────────────────┘
```

Không có CTA `Thanh toán lại`.

## 12.2. SUCCESS

```text
┌──────────────────────────────────────┐
│                ✓                     │
│                                      │
│       Thanh toán thành công          │
│                                      │
│ Booking LAK-AB12CD                   │
│ Movie Title                          │
│ 20:30 · 18/09                        │
│ A5, A6                               │
│                                      │
│ [Xem vé]                             │
│ [Về trang chủ]                       │
└──────────────────────────────────────┘
```

## 12.3. FAILED

```text
┌──────────────────────────────────────┐
│                !                     │
│                                      │
│     Thanh toán chưa thành công       │
│                                      │
│ Giao dịch không được hoàn tất.       │
│                                      │
│ [Thử lại]                            │
│ [Xem booking]                        │
└──────────────────────────────────────┘
```

`Thử lại` chỉ xuất hiện khi backend xác nhận safe.

## 12.4. PAYMENT_REVIEW

```text
┌──────────────────────────────────────┐
│                ◷                     │
│                                      │
│      Đang xác nhận giao dịch         │
│                                      │
│ Chúng tôi đang đối soát giao dịch.   │
│ Không cần thanh toán lại.            │
│                                      │
│ [Xem booking]                        │
└──────────────────────────────────────┘
```

## 12.5. REFUND_PENDING

```text
┌──────────────────────────────────────┐
│                ↺                     │
│                                      │
│        Đang xử lý hoàn tiền          │
│                                      │
│ Thanh toán được ghi nhận sau hạn.    │
│ Vé sẽ không được phát hành.          │
│                                      │
│ [Theo dõi trạng thái]                │
└──────────────────────────────────────┘
```

## 12.6. EXPIRED

```text
┌──────────────────────────────────────┐
│                ⏱                     │
│                                      │
│       Phiên đặt vé đã hết hạn        │
│                                      │
│ Ghế đã được giải phóng.              │
│                                      │
│ [Chọn suất khác]                     │
└──────────────────────────────────────┘
```

---

# 13. C08 — My Tickets

## 13.1. Desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├────────────────────────────────────────────────────────────────────────────┤
│ VÉ CỦA TÔI                                                                 │
│                                                                            │
│ [Sắp xem] [Đã xem] [Đã hủy / hoàn]                                        │
│                                                                            │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ Poster │ Movie Title                                                   │ │
│ │        │ 20:30 · 18/09/2026                                           │ │
│ │        │ LAK Cinema A · Room 2                                        │ │
│ │        │ Ghế A5, A6                                   [VALID]         │ │
│ │        │ Booking LAK-AB12CD                                            │ │
│ │        │                                  [Xem vé] [Yêu cầu hoàn]      │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│ [Ticket Card]                                                              │
└────────────────────────────────────────────────────────────────────────────┘
```

## 13.2. Mobile

```text
┌──────────────────────────────┐
│ ← Vé của tôi                 │
├──────────────────────────────┤
│ [Sắp xem] [Đã xem] [Đã hủy] │
├──────────────────────────────┤
│ [Poster] Movie Title         │
│          20:30 · 18/09       │
│          Cinema A · Room 2   │
│          A5, A6              │
│          [VALID]             │
│                              │
│ [Xem vé]                     │
│ [Yêu cầu hoàn]               │
├──────────────────────────────┤
│ ...                          │
└──────────────────────────────┘
```

## 13.3. Empty

```text
Bạn chưa có vé sắp xem.

[Khám phá phim]
```

---

# 14. C09 — Booking Detail

## Desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ ← Booking LAK-AB12CD                                      [PAID]          │
├───────────────────────────────────────────┬────────────────────────────────┤
│ THÔNG TIN SUẤT CHIẾU                     │ THANH TOÁN                      │
│ Movie Title                               │ VNPay                           │
│ Cinema A · Room 2                         │ 180.000 ₫                       │
│ 20:30 · 18/09                             │ Thành công                      │
│                                           │                                 │
│ GHẾ                                       │ VÉ                              │
│ A5 VIP                                    │ [VALID]                         │
│ A6 VIP                                    │ [Xem QR]                        │
│                                           │                                 │
│ GIÁ                                       │ HOÀN TIỀN                       │
│ Tạm tính ...                              │ Đủ điều kiện / Không đủ         │
│ Tổng ...                                  │ [Yêu cầu hoàn]                  │
└───────────────────────────────────────────┴────────────────────────────────┘
```

Mobile chuyển thành stack sections.

---

# 15. C10 — Refund Request

## 15.1. Step 1 — Eligibility

```text
┌──────────────────────────────────────┐
│ Yêu cầu hoàn vé                     │
│                                      │
│ Booking LAK-AB12CD                  │
│ Movie Title                         │
│ 20:30 · 18/09                       │
│                                      │
│ ✓ Còn trên 45 phút                  │
│ ✓ Vé chưa sử dụng                   │
│ ✓ Booking chưa hoàn                 │
│                                      │
│ [Tiếp tục]                          │
└──────────────────────────────────────┘
```

## 15.2. Step 2 — Reason & Preview

```text
┌──────────────────────────────────────┐
│ Lý do                               │
│ [Chọn lý do ▼]                      │
│                                      │
│ Số tiền dự kiến hoàn                │
│ 180.000 ₫                           │
│                                      │
│ Voucher                             │
│ Sẽ được khôi phục                   │
│                                      │
│ [Quay lại] [Tiếp tục]               │
└──────────────────────────────────────┘
```

## 15.3. Confirmation

```text
┌──────────────────────────────────────┐
│ Xác nhận yêu cầu hoàn               │
│                                      │
│ Booking sẽ được hủy và vé không     │
│ còn hiệu lực sau khi yêu cầu được   │
│ xử lý theo trạng thái nghiệp vụ.    │
│                                      │
│ Hoàn dự kiến: 180.000 ₫             │
│                                      │
│ [Không] [Xác nhận hoàn]             │
└──────────────────────────────────────┘
```

## 15.4. Processing

```text
Đã tiếp nhận
      ↓
Đang xử lý
      ↓
Đã hoàn / Hoàn thất bại
```

---

# 16. C11 — Login

## Desktop

```text
┌──────────────────────────────────────────────────────────────┐
│ LAK                                                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                    ĐĂNG NHẬP                                 │
│                                                              │
│                    Email                                     │
│                    [______________________]                  │
│                                                              │
│                    Mật khẩu                                  │
│                    [______________________]                  │
│                                                              │
│                    [Đăng nhập]                               │
│                                                              │
│                    Quên mật khẩu?                            │
│                    Chưa có tài khoản? Đăng ký                │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

Mobile giữ card full-width với padding 16px.

---

# 17. C12 — Register

```text
┌──────────────────────────────────────┐
│ Đăng ký tài khoản                   │
│                                      │
│ Họ tên                              │
│ [_______________________________]   │
│                                      │
│ Email                               │
│ [_______________________________]   │
│                                      │
│ Mật khẩu                            │
│ [_______________________________]   │
│                                      │
│ Xác nhận mật khẩu                   │
│ [_______________________________]   │
│                                      │
│ [Đăng ký]                           │
│                                      │
│ Đã có tài khoản? Đăng nhập          │
└──────────────────────────────────────┘
```

---

# 18. C13 — Forgot Password

```text
┌──────────────────────────────────────┐
│ Quên mật khẩu                       │
│                                      │
│ Nhập email của bạn. Nếu tài khoản   │
│ tồn tại, hệ thống sẽ gửi hướng dẫn. │
│                                      │
│ [Email___________________________]  │
│                                      │
│ [Gửi hướng dẫn]                     │
└──────────────────────────────────────┘
```

Response không tiết lộ email tồn tại hay không.

---

# 19. C14 — Reset Password

```text
┌──────────────────────────────────────┐
│ Đặt lại mật khẩu                    │
│                                      │
│ Mật khẩu mới                        │
│ [_______________________________]   │
│                                      │
│ Xác nhận mật khẩu                   │
│ [_______________________________]   │
│                                      │
│ [Cập nhật mật khẩu]                 │
└──────────────────────────────────────┘
```

Invalid/expired token có dedicated error state.

---

# 20. C15 — Profile

```text
┌──────────────────────────────────────────────────────────────┐
│ Hồ sơ                                                       │
├──────────────────────────────────────────────────────────────┤
│ Họ tên                                                      │
│ [Nguyễn Văn A____________________________]                  │
│                                                             │
│ Email                                                       │
│ adam@example.com                                            │
│                                                             │
│ Số điện thoại                                              │
│ [09xxxxxxxx____________________________]                    │
│                                                             │
│ [Lưu thay đổi]                                              │
├──────────────────────────────────────────────────────────────┤
│ Đổi mật khẩu                                                │
│ [Mật khẩu hiện tại____________________]                     │
│ [Mật khẩu mới_________________________]                     │
│ [Đổi mật khẩu]                                              │
└──────────────────────────────────────────────────────────────┘
```

---

# 21. S01 — QR Scanner

## Mobile

```text
┌──────────────────────────────┐
│ LAK Staff       Cinema A     │
├──────────────────────────────┤
│                              │
│ ┌──────────────────────────┐ │
│ │                          │ │
│ │          CAMERA          │ │
│ │                          │ │
│ │       ┌──────────┐       │ │
│ │       │ scan box │       │ │
│ │       └──────────┘       │ │
│ │                          │ │
│ └──────────────────────────┘ │
│                              │
│ [Bật đèn]                    │
│ [Nhập mã thủ công]           │
├──────────────────────────────┤
│ Scanner | Lookup | History   │
└──────────────────────────────┘
```

## Result — Valid

```text
┌──────────────────────────────┐
│ ✓ VÉ HỢP LỆ                 │
│                              │
│ Movie Title                  │
│ 20:30 · Room 2              │
│ Ghế A5, A6                  │
│                              │
│ [Quét vé tiếp theo]          │
└──────────────────────────────┘
```

## Result — Used

```text
┌──────────────────────────────┐
│ ⚠ VÉ ĐÃ ĐƯỢC SỬ DỤNG       │
│                              │
│ Quét lần đầu: 19:54         │
│ Scanner: Staff A            │
│                              │
│ [Quét vé tiếp theo]          │
└──────────────────────────────┘
```

## Result — Wrong Cinema

```text
┌──────────────────────────────┐
│ ✕ SAI CHI NHÁNH             │
│                              │
│ Vé thuộc LAK Cinema B       │
│                              │
│ [Quét vé tiếp theo]          │
└──────────────────────────────┘
```

---

# 22. S02 — Manual Ticket Lookup

```text
┌──────────────────────────────┐
│ Nhập mã vé                   │
├──────────────────────────────┤
│ [Ticket / Booking code____]  │
│ [Tra cứu]                    │
├──────────────────────────────┤
│ Movie Title                  │
│ 20:30 · Room 2              │
│ A5, A6                      │
│ [VALID]                      │
│                              │
│ [Xác thực vé]                │
└──────────────────────────────┘
```

---

# 23. S03 — Scan History

```text
┌──────────────────────────────┐
│ Lịch sử quét                 │
├──────────────────────────────┤
│ [Hôm nay ▼] [Kết quả ▼]      │
├──────────────────────────────┤
│ 19:54  LAK-X1  VALID         │
│ 19:52  LAK-X2  USED          │
│ 19:47  LAK-X3  WRONG CINEMA  │
│ ...                          │
└──────────────────────────────┘
```

---

# 24. A01 — Admin Dashboard

## Desktop

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Dashboard                                               │
│                  │                                                         │
│ Dashboard        │ [Doanh thu] [Vé bán] [Pending booking] [Refund alert] │
│ Phim             │                                                         │
│ Rạp & phòng      │ ┌────────────────────────────────────────────────────┐ │
│ Suất chiếu       │ │                 REVENUE CHART                      │ │
│ Bảng giá         │ └────────────────────────────────────────────────────┘ │
│ Booking          │                                                         │
│ Thanh toán       │ ┌────────────────────────┐ ┌─────────────────────────┐ │
│ Hoàn tiền        │ │ Top Movies             │ │ Top Showtimes           │ │
│ Voucher          │ │ ...                    │ │ ...                     │ │
│ Người dùng       │ └────────────────────────┘ └─────────────────────────┘ │
│ Báo cáo          │                                                         │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

KPI card chỉ hiển thị metric có ý nghĩa.

---

# 25. A02 — Movie Management

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Phim                                  [+ Thêm phim]    │
│                  │                                                         │
│                  │ [Tìm phim________________] [Trạng thái ▼] [Thể loại ▼]│
│                  │                                                         │
│                  │ ┌────────────────────────────────────────────────────┐ │
│                  │ │ Poster | Tên phim | Ngày | Rating | Status | ... │ │
│                  │ ├────────────────────────────────────────────────────┤ │
│                  │ │ ...                                               │ │
│                  │ │ ...                                               │ │
│                  │ └────────────────────────────────────────────────────┘ │
│                  │                                            Pagination  │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

Create/edit dùng page hoặc large modal/drawer tùy complexity; không dùng modal nhỏ cho form dài.

---

# 26. A03 — Cinema Management

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Rạp                                   [+ Thêm rạp]     │
│                  │                                                         │
│                  │ [Search_______________] [City ▼] [Status ▼]            │
│                  │                                                         │
│                  │ Cinema A                                                │
│                  │ 123 Address · Active                                    │
│                  │ 5 phòng                                                 │
│                  │ [Xem phòng] [Chỉnh sửa]                                 │
│                  │                                                         │
│                  │ Cinema B                                                │
│                  │ ...                                                     │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

---

# 27. A04 — Auditorium & Seat Layout

## Desktop

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Room 2 — Cinema A                         [Lưu]        │
│                  │                                                         │
│                  │ Tên phòng [Room 2__________] Format [2D ▼]             │
│                  │                                                         │
│                  │                MÀN HÌNH                                  │
│                  │            ═══════════════                              │
│                  │                                                         │
│                  │ A    □ □ □ □   □ □ □ □                                 │
│                  │ B    □ □ □ □   □ □ □ □                                 │
│                  │ C    □ □ □ □   □ □ □ □                                 │
│                  │ D    ═══ ═══   ═══ ═══                                 │
│                  │                                                         │
│                  │ [Thêm hàng] [Seat type ▼]                              │
│                  │                                                         │
│                  │ ⚠ Thay đổi layout có thể ảnh hưởng suất chiếu tương lai│
└──────────────────┴─────────────────────────────────────────────────────────┘
```

Existing showtime/booking safety do backend quyết định; UI phải render restriction.

---

# 28. A05 — Showtime Management

## Desktop

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Suất chiếu                         [+ Tạo suất chiếu]  │
│                  │                                                         │
│                  │ [Ngày ▼] [Rạp ▼] [Phòng ▼] [Phim ▼]                   │
│                  │                                                         │
│                  │ 08:00     10:00     12:00     14:00     16:00          │
│                  │ ───────────────────────────────────────────────────     │
│                  │ Room 1   [Movie A──────]    [Movie B────────]          │
│                  │ Room 2        [Movie C──────────]                      │
│                  │ Room 3   [Movie D────]          [Movie E────]          │
│                  │                                                         │
│                  │ [List view] [Calendar view]                             │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

## Create Showtime

```text
┌──────────────────────────────────────────────┐
│ Tạo suất chiếu                              │
│                                              │
│ Phim                                        │
│ [Movie ▼]                                   │
│                                              │
│ Rạp                                         │
│ [Cinema ▼]                                  │
│                                              │
│ Phòng                                       │
│ [Room ▼]                                    │
│                                              │
│ Bắt đầu                                     │
│ [18/09/2026] [20:30]                        │
│                                              │
│ Kết thúc dự kiến                            │
│ 22:45                                       │
│                                              │
│ Giá                                         │
│ [Price profile ▼]                           │
│                                              │
│ [Hủy] [Tạo suất chiếu]                      │
└──────────────────────────────────────────────┘
```

Conflict:

```text
Phòng 2 đã có suất 19:00–21:10.
Hãy chọn giờ hoặc phòng khác.
```

---

# 29. A06 — Pricing

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Bảng giá                           [+ Price profile]    │
│                  │                                                         │
│                  │ Profile: Default 2026                                  │
│                  │ Effective: 01/09 → 31/12                               │
│                  │                                                         │
│                  │ ┌────────────────────────────────────────────────────┐ │
│                  │ │ Rule | Day | Time | Format | Seat | Amount | Prio │ │
│                  │ ├────────────────────────────────────────────────────┤ │
│                  │ │ ...                                               │ │
│                  │ └────────────────────────────────────────────────────┘ │
│                  │                                                         │
│                  │ [Preview resulting price]                              │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

Override showtime phải có badge rõ:

```text
[OVERRIDE]
```

---

# 30. A07 — Booking Management

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Booking                                                 │
│                  │                                                         │
│                  │ [Booking / email / phone_____________]                 │
│                  │ [Status ▼] [Cinema ▼] [Date ▼]                         │
│                  │                                                         │
│                  │ ┌────────────────────────────────────────────────────┐ │
│                  │ │ Code | Customer | Showtime | Amount | Status | ...│ │
│                  │ ├────────────────────────────────────────────────────┤ │
│                  │ │ ...                                               │ │
│                  │ └────────────────────────────────────────────────────┘ │
│                  │                                            Pagination  │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

Booking detail dùng read-focused layout, không editable raw transaction data.

---

# 31. A08 — Payment Management

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Thanh toán                                              │
│                  │                                                         │
│                  │ [Search transaction________________] [Status ▼]         │
│                  │                                                         │
│                  │ ┌────────────────────────────────────────────────────┐ │
│                  │ │ Booking | Provider | Amount | Status | Paid at    │ │
│                  │ ├────────────────────────────────────────────────────┤ │
│                  │ │ ...                                               │ │
│                  │ └────────────────────────────────────────────────────┘ │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

Không hiển thị provider secret/raw signature.

---

# 32. A09 — Refund Management

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Hoàn tiền                                               │
│                  │                                                         │
│                  │ [Status ▼] [Cinema ▼] [Date ▼]                          │
│                  │                                                         │
│                  │ Refund LAK-R01                    [REQUESTED]           │
│                  │ Booking LAK-AB12CD                                      │
│                  │ 180.000 ₫                                               │
│                  │ Reason ...                                              │
│                  │                                      [Xem chi tiết]    │
│                  │                                                         │
│                  │ Refund LAK-R02                    [FAILED]              │
│                  │ ...                                                     │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

Manual action phải confirmation + audit expectation.

---

# 33. A10 — Voucher Management

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Voucher                              [+ Tạo voucher]   │
│                  │                                                         │
│                  │ [Search_________] [Status ▼] [Date ▼]                  │
│                  │                                                         │
│                  │ CODE      DISCOUNT     USAGE      DATE         STATUS   │
│                  │ LAK20     20%          55/100     ...          ACTIVE   │
│                  │ ...                                                     │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

---

# 34. A11 — User Management

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Người dùng                                              │
│                  │                                                         │
│                  │ [Name / email________________] [Role ▼] [Status ▼]     │
│                  │                                                         │
│                  │ Name | Email | Role | Cinema scope | Status | Actions  │
│                  │ ...                                                     │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

Lock user:

```text
┌──────────────────────────────────────┐
│ Khóa tài khoản?                     │
│                                      │
│ Người dùng sẽ không thể đăng nhập.  │
│                                      │
│ [Hủy] [Khóa tài khoản]              │
└──────────────────────────────────────┘
```

---

# 35. A12 — Reports

```text
┌──────────────────┬─────────────────────────────────────────────────────────┐
│ Sidebar          │ Báo cáo                                                 │
│                  │                                                         │
│                  │ [Date range ▼] [Cinema ▼] [Movie ▼]                    │
│                  │                                                         │
│                  │ [Revenue] [Tickets] [Bookings] [Occupancy]             │
│                  │                                                         │
│                  │ ┌────────────────────────────────────────────────────┐ │
│                  │ │                    CHART                           │ │
│                  │ └────────────────────────────────────────────────────┘ │
│                  │                                                         │
│                  │ Breakdown table                                         │
└──────────────────┴─────────────────────────────────────────────────────────┘
```

---

# 36. Shared Loading Patterns

## 36.1. Full-page loading

Không dùng spinner full-screen nếu có thể skeleton đúng layout.

Ví dụ list:

```text
[████████████] [████]
[████████████] [████]
[████████████] [████]
```

## 36.2. Inline refresh

Khi dữ liệu cũ vẫn usable:

```text
Đang cập nhật...
```

Không blank toàn bộ screen.

---

# 37. Shared Error Patterns

## 37.1. Recoverable error

```text
Không thể tải dữ liệu.
[Thử lại]
```

## 37.2. Forbidden

```text
Bạn không có quyền truy cập nội dung này.
[Quay lại]
```

## 37.3. Not found

```text
Không tìm thấy nội dung.
[Về trang chủ]
```

## 37.4. Rate limited

```text
Bạn đã thao tác quá nhiều lần.
Vui lòng thử lại sau.
```

Không hiển thị raw HTTP status trong primary copy.

---

# 38. Sticky Action Rules

Sticky bottom CTA được phép trên mobile khi:

- action là primary;
- screen dài;
- action phụ thuộc dữ liệu phía trên;
- giữ CTA visible cải thiện conversion/safety.

Ví dụ:

- Movie Detail → `Chọn suất chiếu`;
- Seat Selection → `Tiếp tục`;
- Checkout → `Thanh toán`;
- Refund confirmation → `Xác nhận`.

Sticky area phải tránh che content cuối screen.

---

# 39. Modal / Drawer Rules

## Modal

Dùng cho:

- destructive confirmation;
- short focused task;
- state requiring acknowledgement.

Không dùng modal cho form CRUD dài.

## Drawer

Dùng cho:

- mobile filters;
- booking summary mobile;
- compact contextual controls.

---

# 40. Data Table Rules

Desktop table:

```text
Title + Primary Action
Filters
Table
Pagination
```

Nếu có nhiều action:

```text
[⋮]
```

Không đặt:

```text
[View] [Edit] [Delete] [Refund] [More]
```

trên mọi row.

---

# 41. Mobile Table Rules

Ưu tiên theo thứ tự:

1. giữ table với horizontal scroll nếu relationship giữa cột quan trọng;
2. card list nếu mỗi record có hierarchy rõ;
3. không ẩn critical status.

---

# 42. Responsive Review Matrix

Mỗi screen phải review tối thiểu:

```text
375px
768px
1024px
1280px
```

Seat Selection và Scanner nên kiểm tra thêm thiết bị mobile thấp chiều cao.

---

# 43. Keyboard Flow

Các screen tương tác chính phải có tab order hợp lý.

Ví dụ Seat Selection:

```text
Header
→ seat map controls
→ selectable seats
→ booking summary
→ Continue
```

Không tạo focus trap ngoài modal/dialog.

---

# 44. Screen Reader Expectations

Seat:

```text
Ghế A5, loại VIP, còn trống
```

Showtime:

```text
Suất 20 giờ 30, phòng 2D, giá từ 90 nghìn đồng
```

Payment status:

```text
Thanh toán đang được xác nhận
```

Không chỉ đọc icon.

---

# 45. Agent Implementation Checklist

Trước khi coding một screen, agent phải xác nhận:

```text
[ ] Screen ID và route.
[ ] Role/Auth requirement.
[ ] Main CTA.
[ ] Desktop layout.
[ ] Mobile transformation.
[ ] Required components.
[ ] Loading state.
[ ] Empty state.
[ ] Error state.
[ ] Business-specific state.
[ ] Retry semantics.
[ ] Accessibility.
```

Sau khi code:

```text
[ ] Không thay đổi hierarchy chính.
[ ] Không thêm CTA cạnh tranh với CTA chính.
[ ] Không tự tạo business state.
[ ] Không tự tính authoritative price.
[ ] Không reset hold countdown.
[ ] Không coi payment redirect là success.
[ ] Không coi WebSocket là source of truth.
[ ] Mobile không phải desktop thu nhỏ.
[ ] Focus/keyboard hoạt động.
[ ] Critical status không chỉ dùng màu.
```

---

# 46. Stakeholder Review Checklist

Khi review wireframe, stakeholder không cần đánh giá màu sắc chi tiết.

Cần trả lời:

## Navigation

- Có tìm được chức năng cần thiết không?
- Luồng có tự nhiên không?

## Information hierarchy

- Thông tin quan trọng nhất có nổi bật không?
- Có thông tin dư không?

## Booking

- Có biết đang chọn phim/rạp/suất nào không?
- Ghế và tổng tiền có rõ không?
- Countdown có dễ thấy không?

## Payment

- Có biết trạng thái giao dịch hiện tại không?
- Có nguy cơ user thanh toán lại không?

## Ticket

- Có dễ tìm QR không?
- Có hiểu ticket status không?

## Admin

- Có tìm kiếm/lọc được dữ liệu vận hành nhanh không?
- Destructive action có đủ cảnh báo không?

---

# 47. Priority triển khai wireframe

## Phase 1 — Core Booking

Ưu tiên implement:

```text
C11 Login
C12 Register
C01 Home
C02 Movie List
C03 Movie Detail
C04 Showtime Selection
C05 Seat Selection
C06 Checkout
C07 Payment Result
C08 My Tickets
```

## Phase 2 — Operations

```text
S01 QR Scanner
A01 Dashboard
A02 Movie Management
A03 Cinema Management
A04 Auditorium & Seat Layout
A05 Showtime Management
A06 Pricing
A07 Booking Management
A08 Payment Management
A09 Refund Management
```

## Phase 3 — Supporting UX

```text
C09 Booking Detail
C10 Refund Request
C13 Forgot Password
C14 Reset Password
C15 Profile
S02 Manual Lookup
S03 Scan History
A10 Voucher
A11 Users
A12 Reports
```

Priority này không thay thế dependency trong `task.md`.

---

# 48. Definition of Done cho wireframe implementation

Một wireframe được xem là chuyển thành UI thành công khi:

```text
[ ] Hierarchy giữ đúng.
[ ] CTA chính giữ đúng.
[ ] Layout desktop tương ứng.
[ ] Mobile transformation tương ứng.
[ ] Component lấy từ design system.
[ ] Critical state visible.
[ ] Loading state implemented.
[ ] Empty state implemented nếu cần.
[ ] Error state implemented.
[ ] Conflict/expired/payment states implemented nếu liên quan.
[ ] Responsive không che nội dung.
[ ] Keyboard usable.
[ ] Screen reader semantics có đủ.
[ ] Không dùng màu làm tín hiệu duy nhất.
[ ] Backend vẫn là source of truth.
```

---

# 49. Phạm vi ngoài wireframe

File này không định nghĩa:

- API payload;
- database schema;
- backend transaction;
- payment signature;
- detailed Tailwind class;
- exact animation easing;
- exact icon SVG;
- pixel-perfect visual mockup.

Các nội dung trên phải thuộc tài liệu hoặc layer tương ứng.

---

# 50. Kết luận

Wireframe của LAK được thiết kế theo ba ưu tiên:

```text
1. User luôn biết mình đang ở đâu trong flow.
2. Critical business state luôn nhìn thấy.
3. Primary action luôn rõ nhưng không gây thao tác trùng.
```

Đối với customer flow, ba màn hình cần tuân thủ wireframe chặt nhất là:

```text
C05 Seat Selection
C06 Checkout
C07 Payment Result
```

Đây là ba điểm có rủi ro UX và business cao nhất.

Đối với operations, các màn hình cần ưu tiên tính rõ ràng hơn tính trang trí:

```text
S01 QR Scanner
A05 Showtime Management
A07 Booking Management
A09 Refund Management
```

Mọi implementation khác wireframe đáng kể phải được xem là một design decision mới và cần được review thay vì để coding agent tự quyết định.

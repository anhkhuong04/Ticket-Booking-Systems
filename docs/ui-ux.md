# LAK — UI/UX Design

> Phiên bản: 1.0  
> Phong cách: Clean, professional, light tone  
> Phạm vi: Website khách hàng, trang quản trị và giao diện quét vé.

## 1. Định hướng thiết kế

LAK sử dụng phong cách **light cinema**: nền sáng, nhiều khoảng trắng, card tối giản và màu đỏ làm điểm nhấn thương hiệu.

### Mục tiêu

- Giúp người dùng hoàn thành đặt vé nhanh chóng.
- Trạng thái ghế và thanh toán dễ nhận biết.
- Hiển thị tốt trên desktop và mobile.
- Tạo cảm giác chuyên nghiệp, đáng tin cậy.
- Hình ảnh phim tạo không khí điện ảnh thay vì sử dụng nền tối toàn trang.

### Nguyên tắc

- Ưu tiên nội dung và hành động chính.
- Không sử dụng quá nhiều màu hoặc hiệu ứng.
- Không truyền đạt trạng thái chỉ bằng màu sắc.
- Giá tiền, thời gian giữ ghế và trạng thái booking luôn rõ ràng.
- Giữ thiết kế nhất quán giữa khách hàng, quản trị viên và nhân viên rạp.

---

## 2. Màu sắc

| Token | Giá trị | Mục đích |
|---|---|---|
| `primary` | `#E11D48` | CTA, thương hiệu, ghế được chọn |
| `primary-hover` | `#BE123C` | Hover button |
| `primary-soft` | `#FFF1F2` | Badge và menu đang chọn |
| `background` | `#F8FAFC` | Nền trang |
| `surface` | `#FFFFFF` | Card, modal, header |
| `text-primary` | `#0F172A` | Tiêu đề và nội dung chính |
| `text-secondary` | `#475569` | Nội dung phụ |
| `text-muted` | `#94A3B8` | Placeholder, metadata |
| `border` | `#E2E8F0` | Viền input và card |
| `success` | `#16A34A` | Thanh toán hoặc vé hợp lệ |
| `warning` | `#D97706` | Sắp hết thời gian |
| `error` | `#DC2626` | Lỗi hoặc thanh toán thất bại |
| `info` | `#2563EB` | Thông tin hỗ trợ |

Màu primary chỉ dùng cho hành động quan trọng, không dùng làm màu chữ cho nhiều nội dung vì dễ bị hiểu là cảnh báo.

---

## 3. Typography

```css
font-family: "Be Vietnam Pro", Inter, system-ui, sans-serif;
```

| Cấp độ | Kích thước | Weight |
|---|---:|---:|
| Display | `40/48px` | 700 |
| H1 | `32/40px` | 700 |
| H2 | `24/32px` | 600 |
| H3 | `20/28px` | 600 |
| Body | `16/24px` | 400 |
| Small | `14/20px` | 400–500 |
| Caption | `12/16px` | 500 |

Trên mobile, H1 giảm còn `28–30px`. Mỗi màn hình không nên sử dụng quá ba mức font-weight.

---

## 4. Layout system

### Kích thước chung

- Container tối đa: `1200–1280px`.
- Desktop: grid 12 cột, gutter 24px.
- Tablet: gutter 20px.
- Mobile: padding hai bên 16px.
- Header desktop: 72px.
- Header mobile: 64px.

### Spacing

Sử dụng hệ 4px:

```text
4, 8, 12, 16, 24, 32, 48, 64
```

### Border radius

| Thành phần | Radius |
|---|---:|
| Input, button | `8px` |
| Card | `12px` |
| Modal, drawer | `16px` |
| Badge | `999px` |

Card chủ yếu sử dụng viền `1px`. Shadow chỉ dùng nhẹ khi hover hoặc khi thành phần cần nổi trên nền.

---

## 5. Điều hướng

### Khu vực khách hàng

```text
Logo | Phim | Rạp | Tìm kiếm | Vé của tôi | Tài khoản
```

- Header cố định nhẹ khi cuộn.
- Mobile hiển thị logo, tìm kiếm, vé và menu thu gọn.
- Chi nhánh đang chọn cần hiển thị gần logo hoặc thanh tìm kiếm.

### Khu vực quản trị

Sidebar rộng khoảng 240px:

- Dashboard.
- Phim.
- Rạp và phòng.
- Suất chiếu.
- Bảng giá.
- Booking.
- Thanh toán.
- Hoàn tiền.
- Voucher.
- Người dùng.
- Báo cáo.

### Khu vực nhân viên

- Quét QR.
- Nhập mã vé.
- Lịch sử quét.
- Thông tin chi nhánh.

---

## 6. Trang chủ

### Bố cục

1. Header.
2. Hero phim nổi bật.
3. Đặt vé nhanh.
4. Phim đang chiếu.
5. Phim sắp chiếu.
6. Danh sách chi nhánh.
7. Footer.

### Hero

- Sử dụng gradient sáng kết hợp ảnh phim.
- Có tiêu đề, metadata và CTA “Đặt vé”.
- Không dùng carousel tự động nhanh.
- Nội dung không chiếm toàn bộ chiều cao màn hình.

### Movie card

- Poster tỷ lệ `2:3`.
- Tên phim tối đa hai dòng.
- Hiển thị phân loại tuổi, thể loại và thời lượng.
- CTA chính: “Đặt vé”.
- Desktop: 4–5 card mỗi hàng.
- Mobile: 2 card mỗi hàng.

---

## 7. Trang danh sách phim

- Tab “Đang chiếu” và “Sắp chiếu”.
- Thanh tìm kiếm theo tên phim.
- Lọc theo thể loại, ngày chiếu và phân loại tuổi.
- Bộ lọc desktop hiển thị trên một hàng.
- Mobile sử dụng filter drawer.
- Giữ lại bộ lọc khi người dùng xem chi tiết rồi quay lại.
- Dùng skeleton trong lúc tải.
- Có empty state khi không tìm thấy phim.

---

## 8. Trang chi tiết phim

### Desktop

- Cột trái: poster rộng khoảng 300px.
- Cột phải: tên phim, metadata, mô tả, trailer và CTA.
- Phía dưới: ngày, chi nhánh và suất chiếu.

### Mobile

- Poster và thông tin xếp dọc.
- Nội dung mô tả dài có thể thu gọn.
- Nút “Chọn suất chiếu” cố định phía dưới.

### Thông tin cần hiển thị

- Tên phim.
- Phân loại độ tuổi.
- Thể loại.
- Thời lượng.
- Ngày khởi chiếu.
- Đạo diễn, diễn viên nếu có.
- Mô tả.
- Trailer.
- Lưu ý giấy tờ tùy thân đối với phim giới hạn tuổi.

---

## 9. Chọn suất chiếu

- Thanh ngày dạng tab ngang.
- Nhóm suất chiếu theo chi nhánh.
- Mỗi suất hiển thị giờ, định dạng phòng và giá từ.
- Suất đã đóng bán chuyển sang màu xám.
- Suất sắp hết chỗ có thể hiển thị badge.
- Ghi nhớ chi nhánh người dùng chọn gần nhất.
- Không bắt người dùng chọn lại phim khi đổi chi nhánh.

---

## 10. Chọn ghế

Đây là màn hình cần ưu tiên cao nhất.

### Desktop

```text
Thông tin suất chiếu
Màn hình chiếu
Sơ đồ ghế                  Tóm tắt booking
Chú thích                  Tổng tiền
                           Nút tiếp tục
```

- Sơ đồ ghế chiếm phần lớn màn hình.
- Booking summary cố định bên phải.
- Countdown xuất hiện sau khi backend xác nhận giữ ghế.
- Hiển thị rõ màn hình chiếu và hướng nhìn.
- Hàng ghế và số ghế phải dễ đọc.

### Mobile

- Seat map hỗ trợ kéo và phóng to.
- Booking summary chuyển thành bottom sheet.
- Thanh dưới cùng hiển thị số ghế, tổng tiền và nút tiếp tục.
- Không thu nhỏ trực tiếp bố cục desktop.

### Trạng thái ghế

| Trạng thái | Hiển thị |
|---|---|
| Còn trống | Nền trắng, viền xám |
| Đang chọn | Nền primary, dấu tick |
| Đang giữ | Nền cam nhạt, biểu tượng đồng hồ |
| Đã bán | Nền xám, biểu tượng khóa |
| Bị khóa | Gạch chéo |
| VIP | Badge hoặc viền vàng |
| Ghế đôi | Hình ghế rộng hoặc biểu tượng đôi |

Loại ghế và trạng thái ghế phải có thêm hình dạng, icon hoặc label; không chỉ dựa vào màu.

### UX giữ ghế

- Countdown bắt đầu từ thời gian server.
- Refresh không làm countdown chạy lại.
- Chọn thêm ghế không kéo dài thời gian giữ.
- Ghế bỏ chọn được giải phóng sớm.
- Nếu ghế vừa bị người khác giữ, đánh dấu đúng ghế bị xung đột.
- Ghế đôi luôn được chọn hoặc bỏ chọn cả cặp.

---

## 11. Checkout

### Desktop

- Cột trái: thông tin khách hàng, voucher và phương thức thanh toán.
- Cột phải: thông tin phim, rạp, suất, ghế và tổng tiền.

### Nguyên tắc

- Hiển thị giá từng ghế và phụ thu.
- Không ẩn phí đến bước cuối.
- Countdown luôn nhìn thấy.
- Không yêu cầu nhập lại thông tin đã có.
- Nút thanh toán ghi rõ số tiền.

Ví dụ:

```text
Thanh toán 180.000 ₫
```

- Disable nút sau lần bấm đầu tiên.
- Hiển thị loading khi tạo payment.
- Nếu tạo payment lỗi, cho phép thử lại mà không tạo booking trùng.
- Cảnh báo trước khi người dùng rời trang.

---

## 12. Kết quả thanh toán

Màn hình phải gọi backend để xác nhận; không đọc kết quả trực tiếp từ URL redirect.

### Các trạng thái

| Trạng thái | Nội dung |
|---|---|
| Đang xác nhận | Loading và thông báo không thanh toán lại |
| Thành công | Booking code, vé và nút xem QR |
| Thất bại | Lý do phù hợp và nút thử lại |
| Đang đối soát | Thông báo đã ghi nhận và sẽ tự cập nhật |
| Đang hoàn tiền | Mã giao dịch và trạng thái refund |
| Booking hết hạn | Thông báo ghế đã được giải phóng |

Không hiển thị stack trace, exception hoặc mã lỗi nội bộ cho người dùng.

---

## 13. Vé của tôi

### Tab

- Sắp xem.
- Đã xem.
- Đã hủy hoặc hoàn.

### Vé sắp xem

- Poster và tên phim.
- Ngày giờ.
- Rạp và phòng.
- Danh sách ghế.
- Booking code.
- QR.
- Trạng thái.
- Nút xem chi tiết.
- Nút yêu cầu hoàn nếu còn đủ điều kiện.

QR cần nền trắng và khoảng trống xung quanh để dễ quét. Vé vẫn phải xem được trên website nếu email gửi thất bại.

---

## 14. Yêu cầu hoàn vé

- Hiển thị điều kiện hoàn trước khi xác nhận.
- Cho người dùng chọn lý do.
- Hiển thị số tiền dự kiến được hoàn.
- Thông báo voucher có được khôi phục hay không.
- Yêu cầu xác nhận lần cuối.
- Sau khi gửi, hiển thị tiến trình:

```text
Đã tiếp nhận → Đang xử lý → Đã hoàn / Hoàn thất bại
```

Không dùng từ “Đã hoàn tiền” khi hệ thống mới chỉ ghi nhận yêu cầu.

---

## 15. Admin dashboard

### Tổng quan

- Doanh thu hôm nay.
- Số vé đã bán.
- Booking chờ thanh toán.
- Refund cần xử lý.
- Tỷ lệ lấp đầy.
- Biểu đồ doanh thu.
- Phim và suất chiếu bán tốt.

### Data table

- Header cố định.
- Bộ lọc nằm trên bảng.
- Trạng thái dùng badge.
- Menu thao tác nằm cuối hàng.
- Phân trang phía dưới.
- Không đặt quá nhiều button trực tiếp trên mỗi hàng.
- Cho phép tìm kiếm theo booking code, email hoặc số điện thoại.

Các thao tác hủy suất, thay đổi giá, khóa tài khoản và hoàn tiền phải có modal xác nhận, nêu rõ ảnh hưởng.

---

## 16. Giao diện quét QR

Ưu tiên mobile hoặc PWA.

### Bố cục

- Tên chi nhánh hiển thị cố định.
- Camera chiếm phần lớn màn hình.
- Có nút bật đèn flash.
- Có tùy chọn nhập mã thủ công.
- Phản hồi ngay sau khi quét.

| Kết quả | Hiển thị |
|---|---|
| Hợp lệ | Màu xanh, tên phim và ghế |
| Đã sử dụng | Màu cam, thời gian quét trước |
| Sai chi nhánh | Màu đỏ, chi nhánh hợp lệ |
| Không tồn tại | Màu đỏ |
| Quá thời gian | Màu đỏ hoặc yêu cầu quản lý xác nhận |

Không chỉ dùng âm thanh; cần kết hợp màu, icon và nội dung rõ ràng.

---

## 17. Component chuẩn

- Button: Primary, Secondary, Outline, Ghost, Danger.
- Input.
- Select.
- Date picker.
- Search box.
- Movie Card.
- Showtime Chip.
- Seat.
- Seat Group.
- Booking Summary.
- Countdown.
- Status Badge.
- QR Ticket.
- Modal.
- Drawer.
- Toast.
- Skeleton.
- Empty State.
- Error State.
- Data Table.
- Pagination.
- Confirmation Dialog.

Input và button có chiều cao tối thiểu 44px. Icon sử dụng một bộ thống nhất như Lucide.

---

## 18. Trạng thái component

Mỗi component tương tác cần có:

- Default.
- Hover.
- Focus.
- Active.
- Disabled.
- Loading.
- Error.
- Success nếu phù hợp.

Focus ring đề xuất:

```css
outline: 2px solid #2563EB;
outline-offset: 2px;
```

Loading không được làm layout thay đổi kích thước đột ngột.

---

## 19. Hiệu ứng và chuyển động

| Thành phần | Hiệu ứng |
|---|---|
| Button | Đổi màu trong 150ms |
| Movie card | Nâng 2px và shadow nhẹ |
| Chọn ghế | Scale nhẹ trong 120ms |
| Modal | Fade và scale trong 200ms |
| Drawer | Slide trong 200ms |
| Toast | Slide ngắn trong 200ms |
| Loading | Skeleton nhẹ |

### Không sử dụng

- Parallax mạnh.
- Hiệu ứng 3D phức tạp.
- Carousel tự chạy nhanh.
- Animation trên mọi thành phần.
- Shadow dày.
- Gradient trên quá nhiều khu vực.

Hỗ trợ `prefers-reduced-motion` để tắt chuyển động không cần thiết.

---

## 20. Responsive

| Breakpoint | Mục đích |
|---|---|
| `< 640px` | Mobile |
| `640–767px` | Mobile lớn |
| `768–1023px` | Tablet |
| `1024–1279px` | Desktop |
| `≥ 1280px` | Desktop lớn |

### Quy tắc

- Mobile ưu tiên một cột.
- Bộ lọc chuyển thành drawer.
- Data table phức tạp chuyển thành card list hoặc cuộn ngang có kiểm soát.
- Booking summary chuyển thành bottom sheet.
- CTA quan trọng cố định phía dưới khi cần.
- Seat map phải hỗ trợ pan và zoom.
- Không ẩn thông tin giá hoặc trạng thái quan trọng trên mobile.

---

## 21. Accessibility

- Vùng bấm tối thiểu `44 × 44px`.
- Độ tương phản chữ thông thường tối thiểu 4.5:1.
- Có focus ring khi dùng bàn phím.
- Modal khóa focus và đóng được bằng `Esc`.
- Trạng thái không chỉ thể hiện bằng màu.
- Poster có alt text.
- Input có label rõ ràng.
- Lỗi đặt gần trường dữ liệu tương ứng.
- Ghế có mô tả cho screen reader.

Ví dụ:

```text
Ghế A5, loại VIP, còn trống
```

---

## 22. Luồng UX chính

```text
Chọn phim
→ Chọn rạp và suất chiếu
→ Chọn ghế
→ Kiểm tra đơn hàng
→ Thanh toán
→ Nhận vé
```

### Quy tắc UX

- Ghi nhớ chi nhánh gần nhất.
- Giữ lại bộ lọc khi quay lại.
- Không reset countdown.
- Luôn hiển thị tổng tiền.
- Cho phép quay lại nhưng cảnh báo nếu có thể mất hold.
- Không hướng dẫn thanh toán lại khi giao dịch đang đối soát.
- Email lỗi không làm mất vé.
- Trạng thái refund phải rõ ràng.
- Mỗi màn hình chỉ có một CTA chính nổi bật.

---

## 23. Ưu tiên triển khai

### Mức 1 — Core booking

- Trang chủ.
- Danh sách và chi tiết phim.
- Chọn suất.
- Chọn ghế.
- Checkout.
- Kết quả thanh toán.
- Vé của tôi.

### Mức 2 — Vận hành

- Admin dashboard.
- Quản lý phim, rạp và suất.
- Booking và refund.
- Staff QR scanner.

### Mức 3 — Hoàn thiện

- Empty state và error state.
- Responsive tablet.
- Accessibility.
- Skeleton loading.
- Micro-interaction.
- Kiểm thử usability.

---

## 24. Kết luận

LAK sử dụng một design system chung nhưng có ba mức độ trình bày:

1. **Khách hàng:** thoáng, trực quan và giàu hình ảnh.
2. **Quản trị viên:** rõ dữ liệu, thao tác nhanh và mật độ vừa phải.
3. **Nhân viên quét vé:** tối giản, tập trung vào tốc độ phản hồi.

Ba màn hình cần ưu tiên kiểm thử UX cao nhất là chọn ghế, checkout và kết quả thanh toán. Đây là các điểm dễ làm người dùng mất niềm tin nếu trạng thái, giá tiền hoặc phản hồi không rõ ràng.
---
name: lak-booking-consistency
description: Triển khai hoặc review nghiệp vụ suất chiếu, giữ ghế, booking, payment, refund và ticket của LAK, đặc biệt khi có transaction, concurrency, idempotency hoặc background job.
---

# LAK Booking Consistency

Tuân thủ state machine và deadline trong `docs/design-systems.md`; không tạo trạng thái hay nhánh xử lý mới nếu chưa có yêu cầu.

## Invariant bắt buộc

- PostgreSQL là nguồn dữ liệu quyết định. Redis chỉ hỗ trợ TTL/cache/realtime; lỗi Redis không được làm sai trạng thái ghế.
- Giữ nhiều ghế phải atomic. Trong transaction, khóa `showtime_seats` theo thứ tự ID ổn định, thu hồi hold hết hạn, kiểm tra toàn bộ rồi mới cập nhật; xung đột trả `409`.
- Ghế đôi luôn được kiểm tra, giữ và bán cả cặp.
- Snapshot nhãn ghế, loại ghế và đơn giá vào booking item. Giá phải do backend lấy từ `showtime_prices`.
- Checkout không gia hạn hold. Phân biệt hold deadline và hard deadline; thanh toán đến trễ không được phát hành vé.
- Webhook phải xác minh chữ ký, đối chiếu booking/số tiền/VND và idempotent theo provider event ID. Redirect không xác nhận thanh toán.
- Khóa và cập nhật booking, payment, ghế, ticket/refund trong transaction phù hợp. Chỉ phát hành ticket khi payment hợp lệ và booking chuyển `PAID`.
- Dùng unique/check/exclusion constraint làm lớp bảo vệ cuối; không chỉ dựa vào kiểm tra trong Java.
- Ghi email và event realtime qua transactional outbox. Consumer, job hết hạn, refund retry và reconciliation phải idempotent.

Với thay đổi cạnh tranh cao, bổ sung integration test chạy đồng thời và kiểm chứng rằng chỉ một giao dịch thắng.

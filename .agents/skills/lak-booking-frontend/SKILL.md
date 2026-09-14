---
name: lak-booking-frontend
description: Triển khai hoặc review trải nghiệm chọn suất, chọn ghế, checkout, kết quả thanh toán, vé và refund trên frontend LAK, gồm realtime và countdown.
---

# LAK Booking Frontend

Tuân thủ luồng và trạng thái trong `docs/design-systems.md` cùng `docs/ui-ux.md`. Frontend không được tạo ra trạng thái nghiệp vụ riêng.

## Invariant UX

- Countdown bắt đầu từ thời gian server, không reset khi refresh, retry, chọn thêm ghế hoặc tạo lại payment URL.
- Ghế đôi luôn chọn/bỏ cả cặp. Khi giữ ghế trả xung đột, chỉ rõ ghế lỗi và đồng bộ lại seat map từ backend.
- WebSocket chỉ báo thay đổi; sau event hoặc reconnect phải reconcile với REST API. UI chịu được event trùng, thiếu hoặc sai thứ tự.
- Luôn hiển thị ghế, giá từng ghế, phụ thu, giảm giá và tổng tiền. Không nhận hoặc tự tính giá làm nguồn quyết định thanh toán.
- Ngăn submit lặp trong lúc request chạy; retry phải dùng cơ chế idempotency/API hiện có và không tạo booking trùng.
- Trang payment result luôn hỏi backend. Redirect URL không quyết định thành công; trạng thái đối soát không được hướng dẫn người dùng thanh toán lại.
- Phân biệt rõ pending, paid, failed, expired, payment review và refund progress. Không hiển thị “đã hoàn” khi mới gửi yêu cầu.
- Cảnh báo khi rời checkout có thể mất hold nhưng không dùng dialog gây cản trở ngoài trường hợp cần thiết.
- Vé vẫn truy cập được trong “Vé của tôi” nếu email lỗi; QR có nền trắng và khoảng trống đủ để quét.

Ưu tiên kiểm thử UX cho chọn ghế, checkout và kết quả thanh toán trên cả desktop lẫn mobile.

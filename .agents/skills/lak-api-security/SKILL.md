---
name: lak-api-security
description: Xây dựng hoặc review REST API, Spring Security, JWT, WebSocket và tích hợp ngoài của backend LAK. Dùng khi xử lý controller, xác thực, phân quyền, validation, lỗi API hoặc dữ liệu nhạy cảm.
---

# LAK API & Security

## Quy tắc bắt buộc

- Xác thực và phân quyền tại backend. Kiểm tra cả role lẫn phạm vi chi nhánh cho nhân viên/quản lý; không dựa vào UI.
- Access token sống ngắn; chỉ lưu hash của refresh token, password-reset token và QR token. Password dùng BCrypt hoặc Argon2.
- Request DTO có Bean Validation; chuẩn hóa lỗi nhất quán và không làm lộ stack trace hay chi tiết nội bộ.
- Giữ đúng ngữ nghĩa lỗi đã chốt: `401` chưa xác thực, `403` sai quyền/phạm vi, `409` tranh chấp ghế, `410` hết hạn, `422` vi phạm nghiệp vụ và `429` rate limit.
- Các API giữ ghế, checkout, refund và webhook phải idempotent; khóa idempotency phải gắn với actor, operation và payload phù hợp.
- Xác minh chữ ký webhook trên payload gốc trước khi xử lý. Không log token, password, secret, QR thô hoặc payload nhạy cảm.
- WebSocket chỉ phát thông báo tối thiểu, không chứa dữ liệu cá nhân và không được dùng làm nguồn xác định trạng thái.
- Upload media phải được backend kiểm tra loại/kích thước và ký request. Secret chỉ lấy từ cấu hình môi trường.
- Không thay đổi contract công khai một cách âm thầm; cập nhật tài liệu và test khi endpoint, payload hoặc mã lỗi thay đổi.

Mặc định từ chối khi không chứng minh được quyền hoặc tính hợp lệ của dữ liệu.

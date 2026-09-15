# Email notification qua transactional outbox

Business module không gọi provider email trực tiếp. Trong transaction nghiệp vụ,
module gọi `EmailNotificationQueue.enqueue(aggregateId, payload)`. Request được
ghi thành outbox event `notification.email.requested`; nếu transaction rollback,
event cũng rollback.

## Payload và template

Payload hiện hỗ trợ template `BOOKING_CONFIRMATION` và `PASSWORD_RESET`. Password-reset
payload chỉ chứa `resetUrl` đã được cấu hình phía backend; raw reset token không được ghi log.

```json
{
  "recipient": "customer@example.com",
  "template": "BOOKING_CONFIRMATION",
  "variables": {
    "bookingCode": "LAK-20260915-001",
    "movieTitle": "Tên phim",
    "showtime": "2026-09-15 19:00",
    "seats": "A1, A2"
  }
}
```

Renderer escape HTML cho các biến template. Payload, email người nhận, nội dung
email và token provider không được ghi log.

## HTTP provider contract

Khi `MAIL_ENABLED=true` và `MAIL_PROVIDER=http`, adapter gửi `POST` đến
`MAIL_ENDPOINT` với JSON gồm `from`, `to`, `subject`, `html`. Request chứa:

- `Authorization: Bearer <MAIL_API_TOKEN>`
- `Idempotency-Key: <outbox event UUID>`

Provider phải xử lý `Idempotency-Key` idempotent. Outbox có ngữ nghĩa
at-least-once: lỗi mạng, timeout hoặc non-2xx làm event retry theo backoff chung;
sau số lần tối đa event vào `DEAD_LETTER` để manual review. Không dựa vào log hoặc
response redirect để kết luận email đã được gửi.

Nếu email bị tắt, email handler không được đăng ký; không bật outbox email trong
production trước khi có provider `http` hợp lệ và các biến `MAIL_*` cần thiết.

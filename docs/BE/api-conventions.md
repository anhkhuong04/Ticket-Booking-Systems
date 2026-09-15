# LAK API Conventions

## Request tracing

- Client có thể gửi `X-Request-Id` gồm 1–64 ký tự chữ, số, `.`, `_` hoặc `-`.
- Backend giữ request ID hợp lệ hoặc sinh UUID mới, trả lại trong header `X-Request-Id` và đặt vào MDC dưới tên `request_id`.
- Giá trị không hợp lệ không được đưa vào log để tránh log injection.

## Error response

Mọi lỗi API dùng một envelope ổn định:

```json
{
  "timestamp": "2026-09-15T02:00:00Z",
  "status": 409,
  "code": "SEAT_HOLD_CONFLICT",
  "message": "One or more seats are no longer available",
  "path": "/api/seat-holds",
  "requestId": "19c39531-5e53-4806-9651-2b86eff7979e",
  "violations": []
}
```

`message` là nội dung an toàn cho client. Response không chứa exception, stack trace, SQL, secret hoặc dữ liệu nhạy cảm.

| HTTP | Nhóm lỗi |
|---:|---|
| `400` | Request sai cú pháp, sai kiểu hoặc Bean Validation |
| `401` | Chưa xác thực hoặc credential hết hiệu lực |
| `403` | Sai role hoặc cinema scope |
| `409` | Tranh chấp tài nguyên hoặc request idempotent đang xử lý |
| `410` | Hold/booking đã hết hạn |
| `422` | Vi phạm business rule |
| `429` | Rate limit |

## Pagination, sorting và filter

- Query dùng zero-based `page`, `size` mặc định 20 và tối đa 100.
- Sort dùng `sort=<field>,asc|desc`; endpoint phải allowlist field, không chuyển trực tiếp tên cột từ client vào query.
- Filter dùng tên nghiệp vụ rõ ràng; thời gian dùng ISO-8601 UTC, khoảng thời gian dùng cặp `from`/`to` và quy định inclusive/exclusive trong contract endpoint.
- Response phân trang dùng `content`, `page`, `size`, `totalElements`, `totalPages`.

## Idempotency

- API giữ ghế, checkout và refund nhận header `Idempotency-Key`; webhook dùng thêm provider event ID đã xác minh.
- Key là chuỗi opaque 16–128 ký tự và được scope theo `(actor, operation, key)`. Actor webhook là payment provider đã xác thực.
- Backend lưu hash của method, canonical path và payload chuẩn hóa. Cùng key/cùng payload replay nguyên status và response đã lưu; cùng key/khác payload trả `409 IDEMPOTENCY_KEY_REUSED`.
- Với hai request giống nhau chạy đồng thời, chỉ một request thực thi. Request còn lại chờ có giới hạn để replay; nếu request đầu vẫn chạy thì trả `409 REQUEST_IN_PROGRESS` kèm hướng dẫn retry.
- Record idempotency được ghi cùng transaction nghiệp vụ trong PostgreSQL. TTL phải dài hơn cửa sổ retry của operation; provider event ID của payment tuân thủ retention/audit riêng và không phụ thuộc TTL này.
- Không dùng Redis làm nguồn quyết định idempotency.

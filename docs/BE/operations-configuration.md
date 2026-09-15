# Cấu hình vận hành backend

Backend chọn profile `local` khi không có profile nào được kích hoạt. Dùng
`SPRING_PROFILES_ACTIVE=test` cho test và `SPRING_PROFILES_ACTIVE=prod` cho
môi trường chạy thật. Profile không chứa secret; secret chỉ đi qua environment
hoặc file `.env` local bị Git bỏ qua.

## Biến môi trường

| Nhóm | Biến | Ghi chú |
| --- | --- | --- |
| PostgreSQL | `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Bắt buộc khi chạy backend. |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Password không được ghi vào log. |
| HTTP | `BACKEND_PORT`, `CORS_ALLOWED_ORIGINS` | Chỉ khai báo origin frontend được phép. |
| Logging | `LOG_LEVEL` | Mặc định `INFO`; không đặt `DEBUG` ở production khi không điều tra sự cố. |
| Outbox | `OUTBOX_ENABLED`, `OUTBOX_BATCH_SIZE`, `OUTBOX_POLL_INTERVAL`, `OUTBOX_MAX_ATTEMPTS`, `OUTBOX_INITIAL_BACKOFF`, `OUTBOX_MAX_BACKOFF`, `OUTBOX_PROCESSING_TIMEOUT` | PostgreSQL vẫn là nguồn sự thật; Redis không quyết định trạng thái outbox. |

Tham khảo `.env.example` để tạo `.env` local. Không commit `.env`, token, API
key, password, payload thanh toán hay nội dung email.

## Logging và metrics

Console log theo dạng key-value và luôn có các trường `request_id`,
`booking_code`, `payment_id`. Hai trường business để trống (`-`) khi luồng chưa
có booking hoặc payment. Chỉ đưa vào MDC các giá trị đã validate; không log
email người nhận, token, password, QR thô, nội dung email hoặc payload provider.

Actuator chỉ expose `health`, `info`, `metrics`; health detail không công khai.
Spring Boot cung cấp health indicator cho datasource và Redis. Endpoint
`/api/health` đồng thời ghi metric `lak.dependency.health.checks` theo
`dependency` (`postgresql`/`redis`) và `status` (`up`/`down`).

## Profile

- `local`: dùng `.env` local, log mức `LOG_LEVEL` (mặc định `INFO`).
- `test`: dùng Testcontainers PostgreSQL/Redis, tắt outbox scheduler và giảm log xuống `WARN`.
- `prod`: chỉ nhận cấu hình từ environment/secret store của runtime, log mức `INFO`, health detail vẫn bị ẩn và có xử lý forwarded headers.

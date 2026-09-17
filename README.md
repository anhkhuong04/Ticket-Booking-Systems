# LAK Movie Ticket Booking

## Project Overview

LAK là ứng dụng đặt vé xem phim cho một thương hiệu có nhiều chi nhánh. Khách hàng chọn phim, suất chiếu và ghế, thanh toán để nhận vé QR; nhân viên soát vé và quản trị viên vận hành rạp. Xem [thiết kế hệ thống](docs/BE/design-systems.md) và [đặc tả UI/UX](docs/FE/ui-ux.md) để biết quy tắc chi tiết.

## Features

- **Khách hàng:** xem phim/lịch chiếu, giữ ghế, checkout, thanh toán sandbox, xem vé QR, lịch sử đặt vé và yêu cầu hoàn tiền.
- **Nhân viên:** quét và xác thực vé tại rạp.
- **Quản trị:** quản lý phim, rạp, phòng/ghế, suất chiếu, giá; theo dõi booking, thanh toán, hoàn tiền, người dùng và dashboard.
- **Tài khoản:** đăng ký, đăng nhập, khôi phục mật khẩu; phân quyền theo vai trò và chi nhánh.

## Tech Stack

| Thành phần | Công nghệ |
| --- | --- |
| Frontend | React 19, TypeScript, Vite 8, React Router, Axios, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, WebSocket |
| Dữ liệu | PostgreSQL 17, Redis 7, Flyway |
| Kiểm thử | JUnit, ArchUnit, Testcontainers, Vitest, Testing Library |

## Architecture

Frontend gọi REST API của backend Spring Boot theo kiến trúc modular monolith. Các module chính gồm identity, catalog, cinema, showtime, reservation, booking, payment, refund và ticketing. PostgreSQL quyết định trạng thái ghế, booking và thanh toán; Redis hỗ trợ TTL/realtime, còn WebSocket báo thay đổi để client tải lại dữ liệu từ API. Backend dùng transaction, ràng buộc database và idempotency cho các luồng đặt vé, thanh toán.

## Database

PostgreSQL lưu tài khoản, phim, rạp/phòng/ghế, suất chiếu/giá, giữ ghế, booking, thanh toán, hoàn tiền và vé. Flyway tự chạy migration từ [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration) khi backend khởi động; JPA kiểm tra schema bằng `ddl-auto: validate`. Dữ liệu demo chỉ được tạo ở profile `local` khi `DEMO_SEED_ENABLED=true`.

## API/Swagger

REST API có tiền tố `/api`. Một số nhóm endpoint: `/api/auth`, `/api/movies`, `/api/showtimes`, `/api/seat-holds`, `/api/bookings`, `/api/payments`, `/api/tickets` và `/api/admin`. Kiểm tra sức khỏe tại <http://localhost:8080/api/health>; hợp đồng API được mô tả trong [tài liệu backend](docs/BE/design-systems.md). Dự án hiện chưa cấu hình OpenAPI/Swagger UI.

## How to Run

Yêu cầu **JDK 21**, **Node.js 22+** và **Docker Compose**. Từ thư mục gốc:

```powershell
Copy-Item .env.example .env
```

Đổi các giá trị bí mật mẫu trong `.env` (đặc biệt password PostgreSQL/Redis và `JWT_HMAC_SECRET`), rồi chạy trên Windows:

```powershell
.\scripts\dev.ps1 doctor
.\scripts\dev.ps1 up
.\scripts\dev.ps1 backend
```

Trong terminal khác:

```powershell
cd frontend
npm.cmd ci
cd ..
.\scripts\dev.ps1 frontend
```

Mở <http://localhost:5173>. Backend mặc định chạy ở `localhost:8080`; PostgreSQL và Redis dùng cổng host `5433` và `6380`. Dừng hạ tầng bằng `.\scripts\dev.ps1 down` (giữ nguyên volumes).

Trên macOS/Linux, thay lệnh PowerShell bằng `cp .env.example .env`, `docker compose --env-file .env -f infrastructure/docker-compose.yml up -d`, `cd backend && ./mvnw spring-boot:run` và `cd frontend && npm ci && npm run dev` trong các terminal riêng.

## Testing

```powershell
.\scripts\dev.ps1 verify
```

Lệnh này chạy backend `mvnw verify` (gồm integration/concurrency test dùng Testcontainers) và frontend lint, typecheck, test, build; cần Docker đang chạy. Để chỉ chạy backend unit/architecture test, dùng `cd backend` rồi `cmd /c mvnw.cmd test` trên Windows (hoặc `./mvnw test` trên macOS/Linux).

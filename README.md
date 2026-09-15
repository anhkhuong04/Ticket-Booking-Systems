# LAK Movie Ticket Booking

Nền tảng ban đầu cho hệ thống đặt vé LAK gồm React/TypeScript, Spring Boot, PostgreSQL và Redis. Phiên bản này chỉ triển khai health-check vertical slice, chưa có entity hoặc nghiệp vụ đặt vé.

## Yêu cầu môi trường

- Java 21
- Node.js 22+
- Docker Desktop với Docker Compose

Maven không cần cài riêng vì backend có Maven Wrapper.

## 1. Cấu hình môi trường

Trên Windows, dùng script bootstrap thay vì gọi trực tiếp `npm` hoặc Maven Wrapper. Script tự tìm
JDK 21, dùng `npm.cmd` để tránh Execution Policy của PowerShell và kiểm tra Docker trước khi chạy task:

```powershell
.\scripts\dev.ps1 doctor
```

Từ thư mục gốc, tạo file `.env`:

```powershell
Copy-Item .env.example .env
```

Trên macOS/Linux:

```bash
cp .env.example .env
```

Thay hai giá trị password placeholder trong `.env`. File `.env` đã được Git bỏ qua.

Docker mặc định dùng cổng host `5433` (PostgreSQL) và `6380` (Redis), để không xung đột
với service cục bộ đang dùng `5432`/`6379`.

Nếu `doctor` báo Docker không sẵn sàng, mở Docker Desktop và chờ engine khởi động. Lỗi
`config.json: Access is denied` hoặc `docker_engine: permission denied` là quyền Windows của
tài khoản Docker, không phải lỗi của project: chủ sở hữu máy cần sửa quyền đọc
`%USERPROFILE%\.docker\config.json` và thêm tài khoản vào nhóm `docker-users` (nếu được yêu cầu),
sau đó đăng xuất/đăng nhập lại Windows.

## 2. Khởi động PostgreSQL và Redis

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d
docker compose --env-file .env -f infrastructure/docker-compose.yml ps
```

Chờ hai service hiển thị trạng thái `healthy`.

## 3. Khởi động backend

PowerShell hoặc Command Prompt:

```powershell
.\scripts\dev.ps1 backend
```

macOS/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Flyway tự chạy migration khi ứng dụng khởi động. Health endpoint: <http://localhost:8080/api/health>.

Khi profile `local` khởi động, ứng dụng cũng import idempotent dữ liệu demo để frontend có
thể sử dụng ngay: 35 phim CGV đã xác minh metadata công khai, 3 rạp LAK hư cấu, 9 phòng,
720 ghế, profile/rule giá và lịch chiếu 14 ngày. Tắt bằng `DEMO_SEED_ENABLED=false`.
Seeder chỉ chạy ở profile `local`, không chạy trong `test` hoặc `prod`. Dữ liệu phim nguồn
từ [CGV Now Showing](https://www.cgv.vn/en/movies/now-showing.html) và
[CGV Coming Soon](https://www.cgv.vn/en/movies/coming-soon-1.html); các trường CGV không
công bố (mô tả, poster, trailer) được để trống thay vì suy đoán.

## 4. Khởi động frontend

Trong terminal khác:

```powershell
cd frontend
npm.cmd install
cd ..
.\scripts\dev.ps1 frontend
```

Mở <http://localhost:5173>. Frontend đọc `VITE_API_URL` từ file `.env` ở thư mục gốc và hiển thị trạng thái backend, PostgreSQL và Redis.

## Kiểm tra dự án

Backend:

```powershell
cd backend
cmd /c mvnw.cmd test
```

Lệnh trên chạy unit test và architecture test, không cần Docker. Để chạy thêm integration test và concurrency test với PostgreSQL/Redis tạm thời qua Testcontainers, Docker phải sẵn sàng:

```powershell
.\scripts\dev.ps1 verify
```

Frontend:

```bash
cd frontend
npm run lint
npm run typecheck
npm test
npm run build
```

## Dừng hạ tầng

```powershell
.\scripts\dev.ps1 down
```

Persistent volumes được giữ lại. Chỉ thêm `--volumes` khi chủ động muốn xóa dữ liệu local.

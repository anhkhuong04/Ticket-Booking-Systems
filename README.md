# LAK Movie Ticket Booking

Nền tảng ban đầu cho hệ thống đặt vé LAK gồm React/TypeScript, Spring Boot, PostgreSQL và Redis. Phiên bản này chỉ triển khai health-check vertical slice, chưa có entity hoặc nghiệp vụ đặt vé.

## Yêu cầu môi trường

- Java 21
- Node.js 22+
- Docker Desktop với Docker Compose

Maven không cần cài riêng vì backend có Maven Wrapper.

## 1. Cấu hình môi trường

Từ thư mục gốc, tạo file `.env`:

```powershell
Copy-Item .env.example .env
```

Trên macOS/Linux:

```bash
cp .env.example .env
```

Thay hai giá trị password placeholder trong `.env`. File `.env` đã được Git bỏ qua.

## 2. Khởi động PostgreSQL và Redis

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d
docker compose --env-file .env -f infrastructure/docker-compose.yml ps
```

Chờ hai service hiển thị trạng thái `healthy`.

## 3. Khởi động backend

PowerShell hoặc Command Prompt:

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Flyway tự chạy migration khi ứng dụng khởi động. Health endpoint: <http://localhost:8080/api/health>.

## 4. Khởi động frontend

Trong terminal khác:

```bash
cd frontend
npm install
npm run dev
```

Mở <http://localhost:5173>. Frontend đọc `VITE_API_URL` từ file `.env` ở thư mục gốc và hiển thị trạng thái backend, PostgreSQL và Redis.

## Kiểm tra dự án

Backend:

```powershell
cd backend
./mvnw.cmd verify
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

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml down
```

Persistent volumes được giữ lại. Chỉ thêm `--volumes` khi chủ động muốn xóa dữ liệu local.

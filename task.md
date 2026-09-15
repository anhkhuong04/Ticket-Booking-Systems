# LAK Movie Ticket Booking — Kế hoạch triển khai

> Nguồn phân tích: `overview.md`, `docs/design-systems.md` và `docs/ui-ux.md`.
> Cập nhật tiến độ bằng checkbox và trạng thái; chỉ đánh dấu hoàn tất khi đạt tiêu chí nghiệm thu của task.

## Quy ước quản lý

- Trạng thái: `[ ]` chưa làm, `[-]` đang làm, `[x]` hoàn tất, `[!]` bị chặn.
- Ưu tiên: `P0` bắt buộc cho luồng đặt vé an toàn; `P1` bắt buộc để hoàn tất MVP; `P2` hoàn thiện vận hành; `P3` mở rộng sau MVP.
- Mỗi task phải có migration/API/test tương ứng nếu thay đổi dữ liệu hoặc hành vi backend.
- Mỗi frontend task phải xử lý loading, empty, error, retry, responsive và accessibility; đọc phần liên quan trong `docs/ui-ux.md` trước khi triển khai.
- Sau mỗi task quan trọng: chạy kiểm tra phù hợp, cập nhật file này và tạo một Conventional Commit riêng. Chỉ push khi người dùng yêu cầu.

## Nguyên tắc không được phá vỡ

- PostgreSQL là nguồn sự thật; Redis và WebSocket chỉ hỗ trợ TTL, cache và thông báo realtime.
- Không bán trùng ghế. Giữ nhiều ghế phải atomic; khóa `showtime_seats` theo thứ tự ổn định và dùng database constraint làm lớp bảo vệ cuối.
- Hold kéo dài 5 phút, grace period thanh toán 2 phút, ngừng bán online trước giờ chiếu 5 phút; checkout không gia hạn hold.
- Giá do backend tính từ `showtime_prices`, dùng số nguyên VND và snapshot vào `booking_items`.
- Chỉ webhook đã xác minh mới xác nhận thanh toán; redirect không phải bằng chứng. Thanh toán đến trễ không được phát hành vé.
- Chỉ phát hành một vé sau khi booking chuyển `PAID`; QR chỉ lưu hash; quét thành công chỉ một lần.
- Thời gian lưu UTC bằng `Instant`, hiển thị `Asia/Ho_Chi_Minh`; ID dùng UUID.
- Module giao tiếp qua application interface hoặc domain event, không truy cập repository của nhau.

## Đường găng MVP

`Nền tảng → Chuẩn kỹ thuật → Identity/RBAC → Catalog & Cinema → Showtime & Pricing → Seat Hold → Booking → Ticket foundation → Payment → Ticket API → Scanner/Reporting`

**Task tiếp theo:** `LAK-011 — Chuẩn hóa API và xử lý lỗi`.

## Giai đoạn 0 — Nền tảng chạy end-to-end

- [x] **LAK-000 · P0 — Khởi tạo repository và health-check vertical slice**
  - React/TypeScript/Vite/Tailwind; Java 21/Spring Boot; PostgreSQL; Redis; Flyway; Docker Compose; CI.
  - `GET /api/health`, trang trạng thái hệ thống, CORS, `.env.example` và README đã có.
  - Commit: `f619a03 chore: initialize end-to-end platform`.

## Giai đoạn 1 — Chuẩn kỹ thuật và ranh giới hệ thống

- [x] **LAK-010 · P0 — Hoàn thiện module skeleton và kiểm soát boundary**
  - Bổ sung các module từ system design còn thiếu: `authorization`, `refund`, `notification`, `reporting`, `audit`.
  - Chuẩn hóa cấu trúc `api/application/domain/infrastructure`; chỉ application interface hoặc domain event contract được công khai cho module khác, `common` chỉ chứa technical capability dùng chung.
  - Thêm kiểm thử kiến trúc để ngăn truy cập repository xuyên module, dependency cycle và phụ thuộc vào implementation package của module khác.
  - Hoàn tất khi build pass và các vi phạm boundary đại diện đều bị test phát hiện.

- [ ] **LAK-011 · P0 — Chuẩn hóa API và xử lý lỗi**
  - Định nghĩa response lỗi thống nhất, validation, pagination/filter convention và ánh xạ `400/401/403/409/410/422/429`.
  - Chốt contract `Idempotency-Key`: phạm vi actor/operation, payload hash, xử lý request đồng thời, lưu/replay response và thời hạn lưu phù hợp.
  - Thêm `request_id` xuyên suốt request/response/log; không lộ stack trace hoặc dữ liệu nhạy cảm.
  - Hoàn tất khi có test MVC cho validation và từng nhóm lỗi chính.

- [ ] **LAK-012 · P0 — Nền kiểm thử tích hợp và dữ liệu test**
  - Tạo test profile, integration test PostgreSQL/Redis thật bằng container và fixture tối thiểu.
  - Kiểm tra Flyway từ database rỗng; tách unit, integration và concurrency test để CI chạy ổn định.
  - Hoàn tất khi CI chứng minh migration và test tích hợp chạy lặp lại được.

- [ ] **LAK-013 · P0 — Transactional outbox nền tảng**
  - Migration/repository/job cho `outbox_events`; ghi event cùng transaction nghiệp vụ, publish/retry idempotent.
  - Dùng cho email và WebSocket; theo dõi retry và dead-letter/manual review.
  - Phụ thuộc: LAK-010, LAK-012.

- [ ] **LAK-014 · P1 — Observability và cấu hình vận hành cơ bản**
  - Structured logging với `request_id`, `booking_code`, `payment_id`; metrics/health cho dependency quan trọng.
  - Chuẩn hóa secret qua environment, profile local/test/prod và chính sách log an toàn.
  - Hoàn tất khi có tài liệu cấu hình và test không ghi secret/token vào log.

- [ ] **LAK-015 · P1 — Notification và email adapter nền tảng**
  - Xử lý email từ outbox qua interface provider, template hóa nội dung và retry an toàn.
  - Cấu hình provider bằng environment; không ghi PII, token hoặc nội dung nhạy cảm vào log.
  - Phụ thuộc: LAK-013.

## Giai đoạn 2 — Identity, session và phân quyền

- [ ] **LAK-020 · P0 — Schema identity và bootstrap role**
  - Migration cho `users`, `roles`, `user_roles`, `refresh_tokens`, `password_reset_tokens`.
  - Chuẩn hóa email/phone trước khi lưu; unique có điều kiện trên giá trị chuẩn hóa, hash token, trạng thái tài khoản và seed bốn role đã chốt.
  - Phụ thuộc: LAK-010, LAK-012.

- [ ] **LAK-021 · P0 — Đăng ký, đăng nhập, refresh và logout**
  - Triển khai BCrypt/Argon2, access JWT ngắn hạn, rotation/revoke refresh token và cookie `HttpOnly/Secure/SameSite` theo môi trường.
  - Chốt CORS và cơ chế chống CSRF cho các endpoint dùng cookie ngay trong task này, không trì hoãn lớp bảo vệ nền đến hardening cuối.
  - Rate limit login; không log password/token; test token hết hạn, reuse và tài khoản bị khóa.
  - Phụ thuộc: LAK-020.

- [ ] **LAK-022 · P0 — RBAC theo vai trò**
  - Bảo vệ endpoint theo `CUSTOMER`, `TICKET_STAFF`, `CINEMA_MANAGER`, `SUPER_ADMIN`.
  - Phân quyền tại backend; audit các lần bị từ chối quan trọng và test ma trận role/endpoint.
  - Phụ thuộc: LAK-021.

- [ ] **LAK-023 · P0 — Frontend authentication và route shell theo vai trò**
  - Form đăng ký/đăng nhập; session bootstrap/refresh/logout tập trung; route customer/staff/admin.
  - Guard frontend chỉ phục vụ UX, mọi quyền vẫn do backend quyết định.
  - Phụ thuộc: LAK-021.

- [ ] **LAK-024 · P1 — Quên và đặt lại mật khẩu**
  - Token một lần, có hạn, chỉ lưu hash; gửi liên kết qua outbox/email và vô hiệu hóa sau khi dùng.
  - Trả response không làm lộ email có tồn tại hay không.
  - Phụ thuộc: LAK-021, LAK-015.

## Giai đoạn 3 — Dữ liệu nền: phim, rạp, phòng và ghế

- [ ] **LAK-030 · P0 — Catalog backend và API đọc công khai**
  - Migration/domain cho `movies`, `genres`, `movie_genres`; API danh sách, tìm kiếm/lọc và chi tiết phim.
  - Validate thời lượng, trạng thái phát hành; cache chỉ là tối ưu và phải có chiến lược invalidation.
  - Phụ thuộc: LAK-010, LAK-012.

- [ ] **LAK-031 · P0 — Cinema, auditorium và seat layout backend**
  - Migration/domain cho `cinemas`, `auditoriums`, `seats`; hỗ trợ ghế thường, VIP, ghế đôi và trạng thái khóa.
  - Ràng buộc vị trí ghế duy nhất; ghế đôi có đúng hai ghế cùng `pair_key`.
  - Phụ thuộc: LAK-010, LAK-012.

- [ ] **LAK-032 · P0 — Frontend khám phá phim và rạp**
  - Trang phim đang/sắp chiếu, tìm kiếm/lọc bằng URL state, chi tiết phim và danh sách rạp.
  - Phụ thuộc: LAK-030, LAK-031.

- [ ] **LAK-033 · P0 — Phân quyền theo phạm vi chi nhánh**
  - Migration `staff_cinema_assignments`; enforce cinema scope tại backend cho staff và manager.
  - Hoàn tất khi integration test chứng minh không truy cập chéo chi nhánh.
  - Phụ thuộc: LAK-022, LAK-031.

- [ ] **LAK-034 · P1 — Quản trị catalog và media**
  - API/UI CRUD phim, thể loại; upload Cloudinary do backend ký, kiểm tra loại/kích thước file và audit thay đổi.
  - Phụ thuộc: LAK-022, LAK-030.

- [ ] **LAK-035 · P1 — Quản trị rạp, phòng và sơ đồ ghế**
  - API/UI CRUD rạp, phòng, sơ đồ ghế; giới hạn manager theo chi nhánh.
  - Không cho thay đổi phá vỡ suất chiếu/booking đã phát sinh; test ghế đôi và vị trí trùng.
  - Phụ thuộc: LAK-022, LAK-031, LAK-033.

## Giai đoạn 4 — Suất chiếu và bảng giá

- [ ] **LAK-040 · P0 — Price profile và price rule**
  - Migration/domain cho `price_profiles`, `price_rules`; số tiền `BIGINT`, hiệu lực theo thời gian và priority rõ ràng.
  - Cài thứ tự giá: override suất → bảng giá chi nhánh → mặc định hệ thống.
  - Phụ thuộc: LAK-031.

- [ ] **LAK-041 · P0 — Tạo suất chiếu an toàn**
  - Migration/domain cho `showtimes`, `showtime_prices`, `showtime_seats`.
  - Tính `end_at` gồm thời lượng phim và dọn phòng; dùng PostgreSQL exclusion constraint chống trùng lịch.
  - Khi tạo suất: snapshot ghế và giá; `sales_close_at` trước giờ chiếu 5 phút.
  - Phụ thuộc: LAK-030, LAK-031, LAK-040.

- [ ] **LAK-042 · P0 — API tra cứu suất chiếu và sơ đồ ghế**
  - `GET /api/showtimes` chỉ trả suất còn mở bán; `GET /api/showtimes/{id}/seats` đọc trạng thái từ PostgreSQL.
  - Trả thời gian UTC và contract đủ để frontend hiển thị giờ Việt Nam.
  - Phụ thuộc: LAK-041.

- [ ] **LAK-043 · P0 — Frontend chọn rạp, ngày và suất chiếu**
  - Điều hướng từ phim tới lịch chiếu; hiển thị giờ `Asia/Ho_Chi_Minh` và deep-link bằng URL.
  - Luồng customer không hiển thị suất đã đóng bán, nhất quán với contract của LAK-042 và system design.
  - Phụ thuộc: LAK-032, LAK-042.

- [ ] **LAK-044 · P1 — UI quản trị suất chiếu và bảng giá**
  - CRUD lịch chiếu/giá, cảnh báo xung đột phòng và xác nhận tác vụ ảnh hưởng người mua.
  - Audit đổi giá và các yêu cầu hủy suất; enforce cinema scope.
  - Chưa cho hủy suất có booking đã thanh toán cho đến khi LAK-089 hoàn tất luồng bồi hoàn; không để CRUD đơn giản làm mất quyền lợi người mua.
  - Phụ thuộc: LAK-022, LAK-033, LAK-041.

## Giai đoạn 5 — Giữ ghế an toàn

- [ ] **LAK-050 · P0 — Seat hold transaction và API idempotent**
  - Migration cho `seat_holds`, `seat_hold_items`; `POST/GET/DELETE /api/seat-holds`.
  - Lock ghế theo ID ổn định, thu hồi hold hết hạn, kiểm tra toàn bộ rồi cập nhật atomic; xung đột trả `409`, hết hạn trả `410`.
  - Luôn giữ cả cặp ghế đôi; idempotency key không tạo hold trùng.
  - Phụ thuộc: LAK-021, LAK-042.

- [ ] **LAK-051 · P0 — Expiration job, Redis TTL và WebSocket**
  - Job idempotent chạy mỗi 15–30 giây, PostgreSQL quyết định việc giải phóng hold.
  - Redis TTL là tín hiệu hỗ trợ; phát `SEATS_UPDATED`/`HOLD_EXPIRED` sau commit qua outbox.
  - Hệ thống vẫn đúng khi Redis/WebSocket lỗi.
  - Phụ thuộc: LAK-050, LAK-013.

- [ ] **LAK-052 · P0 — Concurrency test chống bán/giữ trùng ghế**
  - Test nhiều request đồng thời, giữ nhiều ghế rollback toàn bộ, ghế đôi, retry và hold vừa hết hạn.
  - Hoàn tất khi mọi kịch bản chỉ có tối đa một request thắng và database không có trạng thái mâu thuẫn.
  - Phụ thuộc: LAK-050, LAK-051.

- [ ] **LAK-053 · P0 — Frontend sơ đồ ghế và countdown**
  - Hiển thị AVAILABLE/HELD/SOLD/BLOCKED, loại ghế, ghế đôi; chọn ghế có hỗ trợ bàn phím và không chỉ dùng màu.
  - Countdown dùng deadline từ server, không reset khi refresh; WebSocket chỉ kích hoạt đồng bộ lại REST API.
  - Xử lý rõ `409`, `410`, mất kết nối và retry không tạo hold trùng.
  - Phụ thuộc: LAK-050, LAK-051.

## Giai đoạn 6 — Booking, voucher và checkout

- [ ] **LAK-061 · P0 — Booking checkout idempotent**
  - Migration/domain cho `bookings`, `booking_items`; kiểm tra hold thuộc user và còn hạn.
  - Chưa tạo foreign key `voucher_id` khi bảng voucher chưa tồn tại; LAK-063 sẽ bổ sung cột/FK bằng migration tiến tới.
  - Backend lấy giá, snapshot ghế/loại/đơn giá, tạo `PENDING_PAYMENT`, chuyển ghế `PAYMENT_PENDING`; checkout không gia hạn hold.
  - Test idempotency, ownership, deadline, tổng tiền và rollback.
  - Phụ thuộc: LAK-040, LAK-050, LAK-013.

- [ ] **LAK-062 · P0 — Frontend xác nhận đơn và checkout**
  - Hiển thị snapshot ghế, giá, giảm giá, phí và tổng tiền từ backend; không tự tính giá quyết định.
  - Idempotency key ổn định khi retry; bảo toàn lựa chọn và thể hiện hold/payment deadline.
  - Phụ thuộc: LAK-061.

- [ ] **LAK-063 · P1 — Voucher cơ bản**
  - Migration/domain cho `vouchers`, `voucher_redemptions`; bổ sung `bookings.voucher_id` và foreign key bằng migration tiến tới.
  - Hỗ trợ thời hạn, min order, max discount, usage/per-user limit.
  - Áp dụng và ghi nhận voucher trong cùng transaction checkout; chống vượt quota khi concurrent; bổ sung nhập/xóa voucher trên UI.
  - Phụ thuộc: LAK-061.

- [ ] **LAK-064 · P0 — Nền tảng phát hành vé idempotent**
  - Migration/domain cho `tickets`; unique một ticket cho mỗi booking, ticket code unique, QR token ngẫu nhiên mạnh và chỉ lưu `qr_token_hash`.
  - Cung cấp application interface để payment gọi xuyên module trong cùng transaction; chưa phát hành vé nếu chưa có payment hợp lệ.
  - Test concurrent/retry chứng minh một booking chỉ tạo đúng một ticket.
  - Phụ thuộc: LAK-013, LAK-061.

## Giai đoạn 7 — Thanh toán sandbox và xử lý đến trễ

- [ ] **LAK-070 · P0 — Payment abstraction và sandbox adapter**
  - Migration/domain cho `payments`, `payment_events`; interface provider độc lập với VNPay/MoMo.
  - Tạo payment URL từ booking, số tiền VND và deadline do backend cung cấp; secret qua environment.
  - Phụ thuộc: LAK-061.

- [ ] **LAK-071 · P0 — Webhook bảo mật và idempotent**
  - Xác minh chữ ký, provider event ID, transaction ID, booking, amount và currency.
  - Lock booking/payment/ghế; trong hard deadline chuyển payment `SUCCESS`, booking `PAID`, ghế `SOLD`, phát hành ticket qua application interface và ghi event trong một transaction.
  - Unique constraint bảo vệ một payment success cho booking; test webhook lặp, sai chữ ký, sai tiền và đến không đúng thứ tự.
  - Phụ thuộc: LAK-064, LAK-070.

- [ ] **LAK-072 · P0 — Payment deadline, reconciliation và late payment**
  - Khi hết hard deadline mà chưa ghi nhận tiền: booking `PENDING_PAYMENT` → `EXPIRED`, giải phóng ghế an toàn; không chuyển sang review chỉ vì hết hạn.
  - Chấp nhận payment trong grace period nếu ghế vẫn thuộc booking.
  - Chỉ khi payment đã xác minh đến sau hard deadline: payment vẫn ghi `SUCCESS`, booking `EXPIRED` → `PAYMENT_REVIEW`, tuyệt đối không phát vé và ghi sự kiện bồi hoàn.
  - Job đối soát payment pending/mất webhook phải idempotent và tuân thủ cùng state transition.
  - Phụ thuộc: LAK-071.

- [ ] **LAK-073 · P0 — Frontend chuyển hướng và xác nhận trạng thái thanh toán**
  - Redirect page chỉ poll `GET /api/payments/{id}/status`; không tự kết luận thành công từ URL.
  - Xử lý pending, success, failed, timeout và trạng thái chưa chắc chắn; retry không tạo booking/payment trùng.
  - Phụ thuộc: LAK-070, LAK-071.

- [ ] **LAK-074 · P0 — Refund nền tảng cho thanh toán đến trễ**
  - Migration/domain cho `refunds`; provider refund adapter và worker idempotent xử lý sự kiện late payment.
  - Tạo refund `REQUESTED`, chuyển `REFUNDED` khi thành công hoặc `REFUND_FAILED` để xử lý thủ công; giải phóng ghế khi an toàn.
  - Test retry, provider timeout, duplicate callback và bảo đảm không phát hành vé.
  - Phụ thuộc: LAK-072.

## Giai đoạn 8 — Vé QR, email và soát vé

- [ ] **LAK-080 · P0 — API truy xuất vé và kiểm chứng phát hành**
  - API lấy vé theo code có authorization; chỉ chủ booking hoặc vai trò/phạm vi hợp lệ được truy cập.
  - Bổ sung integration test kiểm chứng payment hợp lệ tạo đúng một ticket trong cùng transaction và late payment không tạo ticket.
  - Phụ thuộc: LAK-064, LAK-071.

- [ ] **LAK-081 · P0 — Trang Vé của tôi và lịch sử booking**
  - `GET /api/me/bookings`, chi tiết booking/vé; UI lịch sử và vé QR theo trạng thái thực từ backend.
  - Không cache hoặc hiển thị QR ngoài đúng chủ tài khoản.
  - Phụ thuộc: LAK-080.

- [ ] **LAK-082 · P1 — Email vé và retry**
  - Notification adapter gửi email từ outbox, template vé tối thiểu và endpoint resend có rate limit.
  - Không ghi QR thô hoặc PII vào log; theo dõi retry thất bại.
  - Phụ thuộc: LAK-015, LAK-080.

- [ ] **LAK-083 · P1 — Scanner online cho nhân viên**
  - Migration `ticket_scan_logs`; `POST /api/tickets/validate` kiểm tra vé, chi nhánh, suất chiếu và role/scope.
  - Khóa để chỉ một lần scan thành công; lần quét lại trả kết quả lần đầu mà không cập nhật lần hai.
  - UI scanner có fallback nhập mã và trạng thái rõ ràng.
  - Phụ thuộc: LAK-022, LAK-033, LAK-080.

## Giai đoạn 9 — Refund và vận hành quản trị

- [ ] **LAK-089 · P1 — Hủy suất và bồi hoàn booking an toàn**
  - Hủy/đóng bán suất theo cinema scope, ngăn giao dịch mới và xử lý các booking bị ảnh hưởng bằng workflow idempotent.
  - Booking đã thanh toán được hoàn 100% không phụ thuộc hạn 45 phút; hủy ticket, xử lý voucher theo chính sách đã chốt, ghi outbox và audit.
  - Retry/partial failure không được bỏ sót booking hoặc tạo refund trùng; có trạng thái manual review khi provider refund lỗi.
  - Phụ thuộc: LAK-044, LAK-063, LAK-074, LAK-080.

- [ ] **LAK-090 · P1 — Khách yêu cầu refund toàn bộ booking**
  - API idempotent chỉ hoàn toàn bộ, hoàn 100% và kiểm tra hạn 45 phút, vé chưa dùng, chưa từng hoàn.
  - Khôi phục voucher còn hạn; refund lỗi chuyển manual review.
  - Lock payment/booking/ticket/ghế phù hợp; ticket chuyển `CANCELLED` và audit đầy đủ.
  - Phụ thuộc: LAK-063, LAK-074, LAK-080, LAK-083.

- [ ] **LAK-091 · P1 — Frontend yêu cầu và theo dõi refund**
  - Hiển thị điều kiện, xác nhận tác vụ, trạng thái pending/refunded/failed và lý do từ chối an toàn.
  - Phụ thuộc: LAK-090.

- [ ] **LAK-092 · P1 — Admin quản lý booking/payment/refund/user**
  - Tìm kiếm, lọc, xem chi tiết và thao tác được phép theo role/cinema scope; không cho sửa trực tiếp dữ liệu giao dịch.
  - Audit khóa/mở user, hủy suất, đổi giá và xử lý refund.
  - Phụ thuộc: LAK-022, LAK-033, LAK-089, LAK-090.

- [ ] **LAK-093 · P1 — Dashboard và báo cáo cơ bản**
  - Doanh thu, số booking/vé và tỷ lệ lấp đầy theo thời gian/phim/rạp; query/index phù hợp và timezone đúng.
  - Số liệu chỉ tính từ trạng thái giao dịch đã chốt; kiểm tra reconciliation với dữ liệu nguồn.
  - Phụ thuộc: LAK-071, LAK-083.

## Giai đoạn 10 — Hardening và phát hành MVP

- [ ] **LAK-100 · P0 — Security hardening**
  - Rate limit login/hold/voucher/webhook/scan; CORS/CSP/cookie production; kiểm tra upload, secret và dependency vulnerabilities.
  - Threat-test IDOR, privilege escalation, replay webhook/refresh token, brute force QR và log leakage.
  - Phụ thuộc: hoàn tất các API MVP.

- [ ] **LAK-101 · P0 — End-to-end test luồng doanh thu**
  - Tự động hóa: đăng nhập → chọn phim/rạp/suất → giữ ghế → checkout → webhook → nhận vé → quét vé.
  - Bao phủ conflict ghế, hết hold, refresh/retry, payment trễ, refund, hủy suất và Redis/WebSocket lỗi.
  - Phụ thuộc: LAK-089, LAK-090, LAK-093.

- [ ] **LAK-102 · P1 — Docker hóa ứng dụng và môi trường triển khai**
  - Image frontend/backend non-root, Compose đầy đủ, health/readiness, migration strategy và cấu hình environment.
  - Tài liệu backup/restore PostgreSQL, rollback ứng dụng và xử lý migration lỗi.
  - Phụ thuộc: LAK-101.

- [ ] **LAK-103 · P1 — Monitoring và runbook**
  - Dashboard/cảnh báo cho active hold, hold expiry, payment success, webhook trễ/lỗi, refund lỗi và email retry.
  - Runbook cho payment mismatch, queue backlog, Redis down, database restore và manual refund review.
  - Phụ thuộc: LAK-014, LAK-102.

- [ ] **LAK-104 · P0 — Release candidate và nghiệm thu MVP**
  - CI xanh; migration từ database rỗng và bản trước thành công; E2E/responsive/accessibility/security smoke pass.
  - Dữ liệu demo, README vận hành, API contract và checklist rollback đầy đủ.
  - Phụ thuộc: LAK-100 đến LAK-103.

## Sau MVP — Chưa đưa vào lịch triển khai

- [ ] **LAK-200 · P2 — Voucher nâng cao và audit/reporting chuyên sâu**
- [ ] **LAK-201 · P2 — Tự động hóa reconciliation/refund và cảnh báo vận hành nâng cao**
- [ ] **LAK-300 · P3 — Thành viên/tích điểm, combo bắp nước, SMS và giá theo đối tượng**
- [ ] **LAK-301 · P3 — Chuyển nhượng vé và quét vé offline có đồng bộ**
- [ ] **LAK-302 · P3 — Đánh giá multi-brand khi có yêu cầu kinh doanh thực tế**

## Definition of Done áp dụng cho mọi task

- Phạm vi và dependency đã rõ; không tự mở rộng ngoài MVP.
- Code tuân thủ module boundary, security và invariant nghiệp vụ trong tài liệu thiết kế.
- Migration tiến tới, constraint/index phù hợp và không chỉnh migration đã phát hành.
- Unit/integration/concurrency/E2E test được bổ sung theo mức rủi ro và đều pass.
- Backend build/test; frontend lint/typecheck/test/build; API/docs/README được cập nhật khi liên quan.
- Không có secret, token, QR thô, PII hoặc lỗi nội bộ trong repository/log/response.
- `task.md` được cập nhật trạng thái và có Conventional Commit riêng cho task quan trọng.

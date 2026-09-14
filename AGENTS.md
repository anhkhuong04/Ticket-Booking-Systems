# LAK Project Agent Guide

## Phạm vi và nguồn sự thật

- File này áp dụng cho toàn repository. Quy tắc backend và frontend chỉ áp dụng cho phần tương ứng.
- Trước khi thiết kế hoặc sửa hệ thống, đọc `overview.md` và phần liên quan trong `docs/design-systems.md`.
- Với frontend, đọc thêm phần màn hình liên quan trong `docs/ui-ux.md`.
- `docs/design-systems.md` là nguồn sự thật cho nghiệp vụ, database và API; `docs/ui-ux.md` là nguồn sự thật cho trải nghiệm và giao diện. Không tự thêm nghiệp vụ hoặc thay đổi quyết định đã chốt.
- Yêu cầu trực tiếp của người dùng được ưu tiên. Nếu yêu cầu mâu thuẫn với invariant hệ thống, nêu rõ xung đột và tác động trước khi triển khai.

## Skill routing

Đọc và áp dụng đầy đủ các skill phù hợp trước khi thực hiện:

- Mọi thay đổi Java/Spring backend: `.agents/skills/lak-java-backend/SKILL.md`.
- Code production, migration, test hoặc review backend: `.agents/skills/lak-java-quality/SKILL.md`.
- Showtime, seat hold, booking, payment, refund, ticket, transaction hoặc concurrency: `.agents/skills/lak-booking-consistency/SKILL.md`.
- REST API, JWT, authorization, validation, WebSocket, webhook hoặc dữ liệu nhạy cảm: `.agents/skills/lak-api-security/SKILL.md`.
- Mọi thay đổi React/TypeScript frontend: `.agents/skills/lak-react-frontend/SKILL.md`.
- UI component, layout, Tailwind hoặc responsive design: `.agents/skills/lak-ui-design-system/SKILL.md`.
- Luồng chọn suất/ghế, checkout, payment result, ticket hoặc refund phía client: `.agents/skills/lak-booking-frontend/SKILL.md`.
- Code production, test, review, accessibility hoặc hiệu năng frontend: `.agents/skills/lak-frontend-quality/SKILL.md`.

Một task có thể cần nhiều skill; không bỏ qua skill chuyên biệt chỉ vì đã đọc skill nền tảng.

## Nguyên tắc backend bắt buộc

- Giữ kiến trúc modular monolith và boundary theo business module. Module khác chỉ được gọi qua service interface hoặc domain event, không truy cập trực tiếp repository của nhau.
- PostgreSQL là nguồn dữ liệu quyết định. Redis và WebSocket chỉ hỗ trợ TTL, cache và thông báo realtime.
- Bảo vệ invariant bằng transaction và database constraint; các luồng retry, webhook, job và consumer phải idempotent.
- Không tin giá, quyền, trạng thái thanh toán hoặc trạng thái ghế từ client. Phân quyền role và cinema scope tại backend.
- Không phát hành vé nếu thanh toán chưa được xác minh. Không dùng payment redirect làm bằng chứng thanh toán.
- Lưu thời gian UTC bằng `Instant`, inject `Clock` cho logic thời gian; hiển thị theo `Asia/Ho_Chi_Minh`.
- Dùng UUID cho ID và số nguyên cho tiền VND; không dùng `float` hoặc `double` cho tiền.
- Không trả JPA entity qua API, không field injection, không log secret/token/password/QR thô.

## Nguyên tắc frontend bắt buộc

- Dùng React, TypeScript, Vite, React Router, Axios và Tailwind CSS; giữ TypeScript strict và không thêm dependency không cần thiết.
- Backend là nguồn sự thật cho giá, quyền, ghế, booking và payment. WebSocket chỉ thông báo; client phải đồng bộ lại với API.
- Giữ design token, responsive behavior và accessibility theo `docs/ui-ux.md`; không truyền đạt trạng thái chỉ bằng màu.
- Không reset countdown từ server, không xác nhận thanh toán từ redirect URL và không tạo booking trùng khi retry.
- UI phải thể hiện rõ loading, empty, error, retry và trạng thái chưa chắc chắn; không hiển thị dữ liệu nhạy cảm hoặc lỗi nội bộ.

## Cách làm việc

- Đọc code, test và convention hiện hữu trước khi sửa. Thay đổi nhỏ, đúng phạm vi; không refactor hoặc thêm dependency không cần thiết.
- Khi phải chọn giải pháp, ưu tiên correctness, data integrity, security, khả năng vận hành rồi mới đến sự tiện lợi khi code.
- Schema change phải có migration an toàn. API contract thay đổi phải cập nhật tài liệu và test liên quan ở cả server và client.
- Viết test theo rủi ro, đặc biệt cho transaction, locking, deadline, authorization, idempotency, async state và accessibility.
- Trước khi hoàn tất, chạy compile, test và static checks phù hợp. Báo rõ lệnh đã chạy, kết quả và phần chưa thể xác minh.
- Trình bày kết quả ngắn gọn, đúng trọng tâm; nêu trade-off khi quyết định ảnh hưởng kiến trúc, dữ liệu hoặc API compatibility.

## Commit sau khi hoàn thành

- Sau khi hoàn thành một feature hoặc task quan trọng và các kiểm tra phù hợp đã đạt, tạo một commit riêng cho task đó.
- Trước khi commit, kiểm tra `git status` và diff; chỉ stage file thuộc phạm vi task, không đưa thay đổi sẵn có hoặc không liên quan của người dùng vào commit.
- Dùng Conventional Commits với message ngắn, mô tả đúng mục đích thay đổi, ví dụ `feat: add seat selection flow` hoặc `fix: prevent duplicate checkout`.
- Không commit khi implementation còn dang dở hoặc kiểm tra bắt buộc đang lỗi. Nếu không thể commit vì workspace chưa là Git repository hoặc thiếu cấu hình Git, báo rõ trong kết quả.
- Không amend, squash, rebase hoặc push nếu người dùng chưa yêu cầu rõ ràng.

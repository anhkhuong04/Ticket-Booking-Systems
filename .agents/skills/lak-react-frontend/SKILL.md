---
name: lak-react-frontend
description: Thiết kế, triển khai hoặc chỉnh sửa frontend React/TypeScript của LAK. Dùng cho component, page, routing, state, gọi API và cấu trúc code phía client.
---

# LAK React Frontend

Đọc `overview.md`, phần API liên quan trong `docs/design-systems.md` và phần màn hình liên quan trong `docs/ui-ux.md` trước khi triển khai.

## Quy tắc bắt buộc

- Dùng React, TypeScript, Vite, React Router, Axios và Tailwind CSS; không thêm dependency khi stack hiện có đáp ứng được.
- Tổ chức theo feature/domain. Component UI dùng chung không chứa nghiệp vụ; page điều phối luồng nhưng không chứa chi tiết HTTP.
- Tập trung API client, auth/session và chuyển đổi lỗi ở boundary rõ ràng. Không gọi Axios rải rác trong presentational component.
- Dùng TypeScript strict; tránh `any`, non-null assertion và ép kiểu để che dữ liệu chưa được kiểm chứng.
- Phân biệt server state, URL state và local UI state. Đưa filter có thể chia sẻ/khôi phục vào URL; không sao chép cùng dữ liệu vào nhiều store.
- Không tự suy diễn giá, quyền, trạng thái ghế, booking hoặc payment. Backend là nguồn sự thật; WebSocket chỉ kích hoạt cập nhật hoặc đồng bộ lại.
- Mỗi màn hình phải xử lý loading, empty, error và retry phù hợp. Không làm mất dữ liệu người dùng khi request lỗi.
- Không render dữ liệu nhạy cảm, stack trace hoặc mã lỗi nội bộ. Route guard chỉ hỗ trợ UX, không được coi là lớp phân quyền.

Giữ thay đổi nhỏ, nhất quán với codebase và nêu trade-off nếu quyết định ảnh hưởng routing, state ownership hoặc API contract.

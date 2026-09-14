---
name: lak-frontend-quality
description: Kiểm thử, review, refactor hoặc hoàn thiện chất lượng frontend LAK. Dùng cho accessibility, responsive behavior, hiệu năng, test React/TypeScript và Definition of Done phía client.
---

# LAK Frontend Quality

## Definition of done

- Dùng semantic HTML; điều khiển được bằng bàn phím; focus nhìn thấy và quay về đúng chỗ sau modal/drawer.
- Vùng bấm tối thiểu 44×44px, contrast chữ thường tối thiểu 4.5:1; input có label, lỗi gắn với field và poster có alt phù hợp.
- Modal khóa focus và đóng bằng `Esc`. Ghế có accessible name gồm số ghế, loại và trạng thái; icon/màu không đứng một mình để truyền đạt ý nghĩa.
- Xác minh các breakpoint trong `docs/ui-ux.md`; không ẩn giá, countdown hoặc trạng thái quan trọng trên mobile.
- Giữ layout ổn định khi loading; tránh request race, stale response, memory leak và listener/WebSocket không được cleanup.
- Test hành vi người dùng thay vì implementation detail: navigation, loading/error, form validation, retry, deadline, duplicate submit và quyền bị từ chối.
- Với seat map/payment, thêm integration test cho realtime reconciliation, hold expiry và trạng thái không chắc chắn. Mock đúng boundary, không mock logic cần chứng minh.
- Chạy typecheck, lint, test và build phù hợp trước khi hoàn tất. Chỉ mở rộng kiểm thử khi rủi ro hoặc lỗi phát hiện được yêu cầu.
- Review ưu tiên correctness, accessibility, trạng thái bất đồng bộ, responsive regression và API compatibility; dẫn file/dòng và tác động cụ thể.

Không vô hiệu hóa type checking, lint rule hoặc accessibility check để làm pipeline xanh.

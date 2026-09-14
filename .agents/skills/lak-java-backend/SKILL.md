---
name: lak-java-backend
description: Thiết kế, triển khai hoặc chỉnh sửa backend Java/Spring Boot của LAK. Dùng cho mọi quyết định về module, domain model, service, repository, cấu hình và code Java phía server.
---

# LAK Java Backend

Đọc `overview.md` và phần liên quan trong `docs/design-systems.md` trước khi quyết định kiến trúc. Hai tài liệu là nguồn sự thật; không tự mở rộng ngoài MVP.

## Quy tắc bắt buộc

- Dùng Java 21, Spring Boot và modular monolith kết hợp layered architecture.
- Tổ chức theo business module; trong module tách rõ API, application, domain và infrastructure. Không truy cập repository của module khác; giao tiếp qua service interface hoặc domain event.
- Giữ controller mỏng, use case điều phối nghiệp vụ, domain thể hiện invariant, repository chỉ xử lý persistence.
- Dùng constructor injection; tránh field injection, global mutable state và utility class chứa nghiệp vụ.
- Không trả JPA entity trực tiếp qua API. Dùng request/response DTO và ánh xạ rõ ràng.
- Dùng UUID cho định danh, `Instant` cho thời gian lưu UTC, `Clock` cho logic phụ thuộc thời gian và số nguyên cho tiền VND. Không dùng `double`/`float` cho tiền.
- Validate tại biên hệ thống và tái kiểm tra invariant trong application/domain. Không tin dữ liệu giá, quyền hoặc trạng thái từ client.
- Ưu tiên code đơn giản, dễ đọc và nhất quán với codebase; chỉ thêm abstraction khi có nhu cầu thực tế.
- Không nuốt exception, không trả `null` mơ hồ và không để TODO thay cho nghiệp vụ bắt buộc.

Khi thiết kế mới, ghi ngắn gọn trade-off của quyết định ảnh hưởng transaction, module boundary, schema hoặc API compatibility.

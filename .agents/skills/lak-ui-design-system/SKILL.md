---
name: lak-ui-design-system
description: Tạo hoặc chỉnh sửa giao diện, layout và component trực quan của LAK theo design system. Dùng cho Tailwind, màu sắc, typography, responsive UI và các màn hình customer, admin hoặc staff.
---

# LAK UI Design System

Đọc phần liên quan trong `docs/ui-ux.md`; dùng token và hành vi đã chốt thay vì tự tạo phong cách mới.

## Nguyên tắc giao diện

- Giữ phong cách light cinema: nền sáng, nhiều khoảng trắng, card tối giản, hình phim nổi bật và `#E11D48` chỉ làm điểm nhấn/CTA chính.
- Dùng font `Be Vietnam Pro`, fallback Inter/system; spacing theo hệ 4px, radius 8px cho control, 12px cho card và 16px cho modal/drawer.
- Mỗi màn hình chỉ có một CTA chính nổi bật. Giá, countdown và trạng thái booking/payment phải luôn rõ ràng.
- Xây component từ variant và state chuẩn; không copy chuỗi class dài giữa nhiều nơi. Tận dụng token/theme Tailwind thay vì magic value lặp lại.
- Mobile là bố cục được thiết kế riêng: filter thành drawer, summary thành bottom sheet, CTA quan trọng có thể cố định; không chỉ thu nhỏ desktop.
- Customer UI thoáng và giàu hình ảnh; admin ưu tiên dữ liệu và thao tác; scanner tối giản, phản hồi tức thì.
- Animation ngắn, có mục đích và hỗ trợ `prefers-reduced-motion`; tránh parallax, 3D, shadow dày và carousel chạy nhanh.
- Không truyền đạt trạng thái chỉ bằng màu; kết hợp text, icon, shape hoặc label.

Ưu tiên tái sử dụng component chuẩn đã nêu trong tài liệu trước khi tạo biến thể mới.

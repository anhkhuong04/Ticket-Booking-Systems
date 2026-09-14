---
name: lak-java-quality
description: Kiểm thử, review, refactor hoặc hoàn thiện code Java/Spring Boot trong dự án LAK. Dùng khi tạo code production, migration, test hoặc đánh giá chất lượng thay đổi backend.
---

# LAK Java Quality

## Definition of done

- Đọc code và test liên quan trước khi sửa; giữ convention hiện hữu trừ khi có lý do kỹ thuật rõ ràng để đổi.
- Thay đổi nhỏ, tập trung; không refactor ngoài phạm vi và không thêm dependency nếu JDK/Spring hoặc thư viện hiện có đã đáp ứng tốt.
- Tên thể hiện nghiệp vụ; method ngắn, một mức trừu tượng; comment giải thích lý do/invariant, không kể lại code.
- Mọi schema change đi qua migration có thể triển khai an toàn; thêm constraint và index theo access pattern thực tế.
- Viết test theo rủi ro: unit test cho rule/domain, integration test cho transaction/JPA/security, concurrency test cho giữ ghế và idempotency test cho webhook/job.
- Test cả happy path, boundary deadline, quyền bị từ chối, rollback và retry/duplicate. Không mock phần đang cần chứng minh như locking hoặc database constraint.
- Chạy formatter/static checks, compile và test phù hợp trước khi kết thúc. Không tuyên bố thành công nếu chưa chạy; nêu rõ phần chưa thể xác minh.
- Review ưu tiên correctness, data integrity, security và regression; dẫn file/dòng và mô tả tác động cụ thể.
- Không che lỗi bằng catch rộng, retry vô hạn, vô hiệu hóa validation/test hoặc giảm mức bảo mật.

Khi yêu cầu mâu thuẫn với invariant trong `docs/design-systems.md`, dừng và nêu xung đột trước khi generate code.

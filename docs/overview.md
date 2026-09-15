# LAK – Movie Ticket Booking

## 1. Tổng quan đề tài

**LAK** là website đặt vé xem phim trực tuyến. Hệ thống cho phép người dùng tìm phim, chọn rạp, suất chiếu, ghế ngồi, thanh toán và nhận vé điện tử. Quản trị viên có thể quản lý nội dung phim, lịch chiếu, rạp, phòng chiếu, đơn đặt vé và theo dõi hoạt động kinh doanh.

## 2. Mục đích

- Giúp người dùng đặt vé nhanh chóng mà không cần mua trực tiếp tại quầy.
- Hiển thị chính xác lịch chiếu, sơ đồ ghế và tình trạng ghế theo thời gian thực.
- Hỗ trợ rạp chiếu phim quản lý tập trung phim, suất chiếu, vé và doanh thu.
- Hạn chế tình trạng nhiều người đặt cùng một ghế.

## 3. Tech stack

### Frontend

- **React + TypeScript + Vite:** xây dựng giao diện web.
- **React Router:** điều hướng giữa các trang.
- **Axios:** gọi REST API từ backend.
- **Tailwind CSS:** thiết kế giao diện responsive.

### Backend

- **Architecture:** Modular Monolith kết hợp Layered Architecture
- **Java 21 + Spring Boot:** xây dựng REST API và xử lý nghiệp vụ.
- **Spring Security + JWT:** xác thực và phân quyền theo bốn vai trò `CUSTOMER`, `TICKET_STAFF`, `CINEMA_MANAGER`, `SUPER_ADMIN`.
- **Spring Data JPA:** truy cập và thao tác dữ liệu.
- **Bean Validation:** kiểm tra dữ liệu đầu vào.
- **WebSocket:** cập nhật trạng thái ghế theo thời gian thực.

### Hạ tầng và tích hợp

- **PostgreSQL:** lưu trữ dữ liệu hệ thống.
- **Redis:** giữ ghế tạm thời và tự động giải phóng khi hết thời gian.
- **VNPay/MoMo sandbox:** mô phỏng thanh toán trực tuyến.
- **Cloudinary:** lưu poster và hình ảnh phim.
- **Docker:** đóng gói và triển khai hệ thống.

Hệ thống sử dụng kiến trúc **Modular Monolith**, phù hợp với phạm vi đồ án và dễ bảo trì hơn Microservices.

## 4. Tính năng triển khai

### 4.1. Phía người dùng

- Đăng ký, đăng nhập, đăng xuất và quên mật khẩu.
- Xem phim đang chiếu, sắp chiếu và thông tin chi tiết phim.
- Tìm kiếm, lọc phim theo thể loại hoặc ngày chiếu.
- Chọn rạp, ngày chiếu và suất chiếu.
- Xem sơ đồ phòng; phân biệt ghế thường, VIP và ghế đôi.
- Xem trạng thái ghế: còn trống, đang được giữ hoặc đã bán.
- Giữ ghế trong thời gian giới hạn khi thực hiện thanh toán.
- Xác nhận thông tin đặt vé, áp dụng voucher và thanh toán trực tuyến.
- Nhận vé điện tử có mã QR sau khi thanh toán thành công.
- Xem lịch sử đặt vé và trạng thái vé.
- Cập nhật thông tin cá nhân và đổi mật khẩu.

### 4.2. Phía quản trị và vận hành

- Xem dashboard về số vé, lượt đặt và doanh thu.
- Quản lý phim, thể loại, poster, trailer và trạng thái phát hành.
- Quản lý rạp, phòng chiếu và sơ đồ ghế.
- Tạo và quản lý suất chiếu; kiểm tra trùng lịch phòng.
- Quản lý đơn đặt vé và trạng thái thanh toán.
- Quản lý voucher, thời hạn và giới hạn sử dụng.
- Quản lý tài khoản; khóa/mở khóa và phân quyền.
- Tra cứu vé bằng mã vé hoặc thông tin người dùng.
- Xem báo cáo doanh thu theo thời gian, phim hoặc rạp.

Các chức năng quản trị được phân quyền theo phạm vi: `CINEMA_MANAGER` chỉ thao tác trong chi nhánh được gán; `SUPER_ADMIN` quản trị toàn hệ thống.

### 4.3. Phía nhân viên soát vé

- Quét mã QR và tra cứu vé thủ công tại rạp được phân công.
- Xem kết quả kiểm vé, bao gồm vé hợp lệ, đã sử dụng hoặc không thuộc đúng rạp.
- Không được truy cập dữ liệu hoặc thực hiện thao tác ngoài phạm vi chi nhánh.

## 5. Luồng nghiệp vụ chính

`Chọn phim → Chọn rạp và suất chiếu → Chọn ghế → Giữ ghế → Thanh toán → Phát hành vé QR`

## 6. Yêu cầu quan trọng

- Không cho phép hai người mua cùng một ghế trong cùng suất chiếu.
- Ghế đang giữ phải tự động được giải phóng khi người dùng không thanh toán đúng hạn.
- Chỉ phát hành vé sau khi hệ thống xác nhận thanh toán thành công.
- Mỗi vai trò chỉ được truy cập đúng chức năng đã phân quyền.
- Giao diện phải dễ sử dụng và hiển thị tốt trên máy tính lẫn điện thoại.

> Tài liệu này chỉ mô tả tổng quan, công nghệ và chức năng của hệ thống; chưa bao gồm phân tích hoặc thiết kế cơ sở dữ liệu. Chi tiết nghiệp vụ, database, API và phân quyền được chốt tại `docs/BE/design-systems.md`; UI/UX và wireframe được chốt lần lượt tại `docs/FE/ui-ux.md` và `docs/FE/wireframes.md`.

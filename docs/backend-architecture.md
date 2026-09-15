# LAK Backend Architecture

Backend được tổ chức theo modular monolith. Mỗi business module sở hữu dữ liệu và implementation của chính nó; module khác chỉ được gọi qua application contract hoặc domain event contract.

## Business modules

`identity`, `authorization`, `catalog`, `cinema`, `showtime`, `reservation`, `booking`, `payment`, `refund`, `ticketing`, `notification`, `reporting` và `audit`.

`common` không phải business module. Package này chỉ chứa technical capability dùng chung như cấu hình, API error, request tracing và transactional outbox; không đặt business rule hoặc repository của business module vào đây.

## Cấu trúc module

```text
<module>/
├── api             HTTP/WebSocket adapter và request/response DTO
├── application     Use case, input/output port và contract gọi xuyên module
├── domain          Aggregate, value object, invariant và domain event
└── infrastructure  JPA repository, provider client và adapter kỹ thuật
```

Chiều phụ thuộc trong một module:

```text
api → application → domain
infrastructure → application/domain
```

- `domain` không phụ thuộc Spring, JPA, API, application hoặc infrastructure.
- `application` không phụ thuộc API hoặc infrastructure.
- `api` không truy cập infrastructure trực tiếp.
- Class thuộc `api`, `infrastructure` và domain implementation của một module không được module khác truy cập.
- Domain event dùng xuyên module đặt trong `domain.event`; application contract dùng xuyên module đặt trong `application` và không làm lộ JPA entity.
- Không tạo dependency cycle giữa các top-level module.

Các quy tắc trên được kiểm tra tự động trong `ArchitectureTests`.

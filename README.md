# Aristino Backend

REST API cho **Aristino**, website thương mại điện tử thời trang nam tích hợp chatbot tư vấn bằng AI. Xây dựng bằng Spring Boot 3, Spring Security (JWT) và MySQL.

> Repo frontend: [aristino-frontend](https://github.com/vhd240305/aristino-frontend)

## Tính năng

**Khách hàng**
- Đăng ký / đăng nhập bằng JWT
- Xem, tìm kiếm, lọc sản phẩm (có biến thể size/màu) và phân trang
- Giỏ hàng, sổ địa chỉ, đặt hàng và theo dõi đơn hàng
- Hủy đơn; quy trình yêu cầu trả hàng / hoàn tiền
- Đánh giá và chấm điểm sản phẩm
- Thanh toán online (VNPay / MoMo) **mô phỏng** bằng cổng thanh toán giả lập, bên cạnh thanh toán khi nhận hàng (COD)
- Chatbot tư vấn mua sắm bằng AI (Gemini API)

**Quản trị viên**
- Quản lý sản phẩm (biến thể, tải ảnh lên), danh mục, đơn hàng và người dùng
- Dashboard thống kê: doanh thu theo ngày, sản phẩm bán chạy, tỷ lệ trạng thái đơn hàng

## Điểm nổi bật

- **Bảo mật:** xác thực JWT stateless, mã hóa mật khẩu BCrypt, phân quyền theo vai trò (`ROLE_ADMIN`, `ROLE_CUSTOMER`) cho toàn bộ `/api/admin/**`, kiểm tra quyền sở hữu để người dùng chỉ truy cập được địa chỉ và đơn hàng của mình.
- **Toàn vẹn tồn kho:** đặt hàng chạy trong transaction và rollback khi thiếu hàng; biến thể sản phẩm dùng optimistic locking (`@Version`), còn bước xác nhận thanh toán dùng pessimistic lock để tránh xử lý trùng.
- **Tự hủy đơn quá hạn:** scheduled job tự động hủy đơn thanh toán online chưa trả sau 30 phút và hoàn lại tồn kho.
- **Chatbot AI:** câu trả lời dựa trên dữ liệu sản phẩm và FAQ thật trong database; không gửi thông tin cá nhân của khách cho nhà cung cấp AI; giới hạn tốc độ theo từng người dùng và toàn hệ thống; tự động chuyển sang bộ trả lời theo luật khi AI không khả dụng hoặc chưa cấu hình API key.
- **Lọc động:** truy vấn sản phẩm và trang quản trị xây dựng bằng JPA Specification.
- **Kiến trúc rõ ràng:** phân lớp Controller → Service → Repository → DTO, validate đầu vào và xử lý lỗi tập trung bằng `GlobalExceptionHandler`.

## Công nghệ

| Lớp | Công nghệ |
|---|---|
| Ngôn ngữ / Framework | Java 17, Spring Boot 3.3 |
| Bảo mật | Spring Security, JWT (jjwt), BCrypt |
| Truy cập dữ liệu | Spring Data JPA (Hibernate), MySQL |
| AI | Google Gemini API |
| Build | Maven |

## Hướng dẫn chạy

### Yêu cầu
- JDK 17 trở lên
- Maven 3.9 trở lên (hoặc chạy từ IDE)
- MySQL 8 trở lên

### 1. Tạo database
Ứng dụng dùng `ddl-auto: validate` nên các bảng phải có sẵn trước khi chạy.

```sql
CREATE DATABASE aristino_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau đó import cấu trúc bảng:

```bash
mysql -u root -p aristino_db < database/schema.sql
```

Thêm 2 vai trò (bắt buộc để đăng ký tài khoản hoạt động):

```sql
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_CUSTOMER');
```

### 2. Cấu hình ứng dụng

```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

Mở `application.yml`, điền mật khẩu MySQL của bạn và đặt `jwt.secret` là một chuỗi ngẫu nhiên dài (ít nhất 32 ký tự).

### 3. (Tùy chọn) Bật chatbot AI
Cung cấp Gemini API key qua biến môi trường:

```bash
# Linux / macOS
export GEMINI_API_KEY=your_key_here

# Windows PowerShell
$env:GEMINI_API_KEY="your_key_here"
```

Nếu không có key, chatbot vẫn hoạt động bằng bộ trả lời theo luật.

### 4. Chạy ứng dụng

```bash
mvn spring-boot:run
```

Hoặc chạy class `AristinoBackendApplication` từ IDE. API chạy tại `http://localhost:8080`.

### 5. Tạo tài khoản admin đầu tiên
Đăng ký một tài khoản thường qua frontend (hoặc endpoint đăng ký trong `/api/auth`), sau đó nâng quyền trong MySQL:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.id = 1 AND r.name = 'ROLE_ADMIN';
```

Thay `u.id = 1` bằng id của tài khoản bạn vừa đăng ký.

## Cấu hình

| Thuộc tính | Mô tả | Mặc định |
|---|---|---|
| `spring.datasource.*` | Kết nối MySQL | – |
| `jwt.secret` / `jwt.expiration` | Khóa ký token / thời hạn (ms) | – / `86400000` |
| `GEMINI_API_KEY` | Gemini API key (biến môi trường) | rỗng (dùng bộ trả lời theo luật) |
| `app.chat.user-messages-per-minute` | Số tin nhắn chat tối đa mỗi người dùng mỗi phút | `10` |
| `app.chat.ai-calls-per-minute` | Số lần gọi AI tối đa toàn hệ thống mỗi phút | `8` |
| `app.order.payment-timeout-minutes` | Thời gian hết hạn đơn thanh toán online chưa trả | `30` |
| `app.payment.mock-enabled` | Dùng cổng thanh toán giả lập | `true` |
| `app.upload.dir` | Thư mục lưu ảnh sản phẩm tải lên | `uploads` |

CORS được cấu hình cho frontend ở môi trường phát triển tại `http://localhost:5173`.

## Tổng quan API

| Nhóm | Đường dẫn gốc |
|---|---|
| Xác thực | `/api/auth` |
| Sản phẩm và đánh giá | `/api/products` |
| Danh mục | `/api/categories` |
| Giỏ hàng | `/api/cart` |
| Đơn hàng | `/api/orders` |
| Thanh toán (giả lập) | `/api/payments` |
| Địa chỉ | `/api/addresses` |
| Hồ sơ người dùng | `/api/users` |
| Chatbot | `/api/chat` |
| Quản trị (yêu cầu `ROLE_ADMIN`) | `/api/admin/products`, `/categories`, `/orders`, `/users`, `/stats`, `/uploads` |

## Cấu trúc dự án

```
src/main/java/com/aristino/backend/
├── config/        Cấu hình web / tài nguyên
├── controller/    REST controller
├── dto/           Đối tượng request / response
├── entity/        JPA entity
├── exception/     Exception tùy chỉnh + GlobalExceptionHandler
├── repository/    Spring Data repository
├── security/      JWT filter, SecurityConfig
├── service/       Logic nghiệp vụ (đơn hàng, thanh toán, chat, thống kê, ...)
└── spec/          JPA Specification để lọc động
```

## Lưu ý

- Thanh toán online (VNPay / MoMo) chỉ là mô phỏng phục vụ demo, không kết nối nhà cung cấp thanh toán thật.
- Dự án được xây dựng cho mục đích học tập và portfolio.

## Tác giả

**Vũ Huy Đạt !** – [GitHub](https://github.com/vhd240305)

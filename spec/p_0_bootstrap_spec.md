# Spec Giai Đoạn P0 — Bootstrap & Config (Spring Boot + Thymeleaf + H2)

> Namespace: **`org.example.todolist`**  
> Repo hiện tại: skeleton Spring Boot (web + data-jpa + security + h2) và `application.properties`.

Mục tiêu P0: Chuẩn hoá cấu trúc dự án, bổ sung dependency Thymeleaf, cấu hình H2 theo profile, tạo layout & trang chủ (placeholder), đảm bảo app chạy ổn để sang P1.

---

## 0) Phạm vi & Kết quả mong đợi
**Trong phạm vi**
- Bổ sung dependency **Thymeleaf** (chưa có trong `pom.xml` hiện tại).
- Thiết lập **profiles** bằng `.properties` (`dev`, `test`) và cấu hình H2.
- Tạo **cấu trúc thư mục Layered** (rỗng/skeleton) theo `org.example.todolist`.
- Tạo **base layout** Thymeleaf + partial flash.
- Tạo **HomeController** và trang chủ `/` (placeholder): liên kết đến `/login` và `/tasks`.

**Definition of Done (P0)**
- `mvn spring-boot:run -Dspring-boot.run.profiles=dev` chạy OK.
- Truy cập `/` hiển thị trang chào mừng (SSR, layout hoạt động, flash hoạt động).
- Truy cập `/h2-console` (dev) được, kết nối DB OK.
- Cấu trúc package sẵn sàng cho P1–P5.

---

## 1) Cập nhật Dependencies (pom.xml)
**Bắt buộc**
- `spring-boot-starter-thymeleaf`

**Khuyến nghị (tuỳ chọn P0)**
- `spring-boot-devtools` (runtimeOnly) để hot reload khi dev.
- `spring-boot-starter-validation` (sẽ dùng ở P1+ cho @Valid; có thể thêm luôn ở P0).

> Ghi chú: Hiện pom đã có `web`, `data-jpa`, `security`, `h2`, `test`, `security-test`.

---

## 2) Cấu trúc thư mục (skeleton)
```
src/main/java/org/example/todolist/
  config/                  # Cấu hình chung (WebConfig,...)
  security/                # (sẽ dùng ở P1)
  domain/
    entity/                # (User/Task/Subtask ở P1/P2)
    dto/
    enums/
  repository/              # (P2)
  service/                 # (P1+)
  web/
    HomeController.java    # Trang chủ, health, redirect
  exception/               # GlobalExceptionHandler (P4)
  TodolistApplication.java

src/main/resources/
  templates/
    _layout.html
    fragments/_flash.html
    home/index.html
  static/
    css/app.css
    js/app.js
  application.properties             # base
  application-dev.properties         # dev profile
  application-test.properties        # test profile
```

---

## 3) Cấu hình `.properties`
### 3.1 `application.properties` (base)
```
spring.application.name=todolist
# Cho phép Thymeleaf cache off ở dev qua profile, base để trống/ít config.
```

### 3.2 `application-dev.properties`
```
# H2 (file hoặc in-memory) — chọn một trong hai
# File mode (giữ dữ liệu giữa các lần chạy):
spring.datasource.url=jdbc:h2:file:./data/devdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
# In-memory (sạch mỗi lần restart):
# spring.datasource.url=jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL

spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Thymeleaf\ n# (mặc định caching on trong prod, dev có thể tắt nếu muốn):
spring.thymeleaf.cache=false

# Session timeout (ví dụ 30 phút)
server.servlet.session.timeout=30m

# Cho phép HiddenHttpMethodFilter để hỗ trợ PUT/DELETE qua form (khi cần)
spring.mvc.hiddenmethod.filter.enabled=true
```

### 3.3 `application-test.properties`
```
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

server.servlet.session.timeout=30m
```

**Kích hoạt profile**
- Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- Hoặc: `SPRING_PROFILES_ACTIVE=dev` khi chạy jar.

---

## 4) Controllers & Routes (P0)
- `GET /` → `home/index.html` (welcome + link đến `/login` & `/tasks`).
- (Tuỳ chọn) `GET /health` trả 200 và text "OK" để check nhanh.

**HomeController (skeleton)**
- `@Controller`, `@GetMapping("/")` trả view `home/index`.

---

## 5) View (Thymeleaf) cơ sở
- `templates/_layout.html`: khung `<head>`, navbar (logo, liên kết Home/Login/Tasks/Profile/Logout — nút nào chưa có thì để disabled hoặc hidden tuỳ login), `<div class="container">` cho `th:insert` content.
- `templates/fragments/_flash.html`: in flash message `success`, `error`.
- `templates/home/index.html`: trang chào, mô tả ngắn dự án, 2 nút: **Login** và **Go to Tasks**.

**Yêu cầu hiển thị**
- Header hiển thị tên app (`todolist`).
- Nếu có principal: hiển thị username (P1 sẽ có), có nút Logout dạng form POST (để sẵn placeholder, có thể ẩn ở P0).

---

## 6) Logging & H2 Console
- Đảm bảo vào `/h2-console` ở profile `dev` → connect URL đúng như trong `application-dev.properties`.
- (Tuỳ chọn) thêm `logging.level.org.hibernate.SQL=debug` khi debug query (chỉ dev).

---

## 7) Kiểm thử khởi động (Smoke tests)
- `@SpringBootTest` → `contextLoads()` pass (hiện đã có class test trống).
- Truy cập thủ công: `/` hiển thị layout + trang home.
- `/h2-console` vào được, test tạo bảng khi thêm entity ở P1/P2.

---

## 8) Phân rã Task nhỏ (cho AI Agent)
**T0.1 — Thêm dependency**
- [x] Thêm `spring-boot-starter-thymeleaf` vào `pom.xml`.
- [x] (Tuỳ chọn) Thêm `spring-boot-devtools` (runtimeOnly) và `spring-boot-starter-validation`.

**T0.2 — Profiles & Properties**
- [x] Tạo `application-dev.properties` và `application-test.properties` theo mẫu.
- [x] Cập nhật `application.properties` (base) tối giản.
- [x] Xác nhận chạy dev profile OK.

**T0.3 — Tạo cấu trúc package**
- [x] Tạo các package trống: `config/`, `security/`, `domain/{entity,dto,enums}`, `repository/`, `service/`, `web/`, `exception/`.

**T0.4 — HomeController + Routes**
- [x] Tạo `HomeController` với `GET /` trả view `home/index`.
- [x] (Tuỳ chọn) `GET /health` trả "OK".

**T0.5 — Thymeleaf Layout & Pages**
- [x] Tạo `templates/_layout.html` (navbar + `th:insert` vùng content).
- [x] Tạo `templates/fragments/_flash.html`.
- [x] Tạo `templates/home/index.html` (nội dung chào + 2 nút Login/Tasks).
- [x] Tạo `static/css/app.css`, `static/js/app.js` (file rỗng ban đầu).

**T0.6 — H2 Console & Verify**
- [x] Bật H2 console ở dev, kiểm tra truy cập `/h2-console`.
- [x] Kết nối tới DB đúng URL cấu hình.

**T0.7 — Smoke Tests & README**
- [ ] Đảm bảo `contextLoads()` pass.
- [ ] README: hướng dẫn chạy P0 (profile dev, truy cập `/` và `/h2-console`).

---

## 9) Acceptance Criteria chi tiết (Checklist)
- [ ] App khởi động profile `dev` không lỗi.
- [ ] Trang `/` render qua `_layout.html` và có flash fragment.
- [ ] Có thể truy cập `/h2-console` và login DB thành công.
- [ ] Cấu trúc package đúng như skeleton; sẵn sàng thêm code ở P1.
- [ ] README mô tả cách chạy và mục tiêu P0.

---

## 10) Rủi ro & Lưu ý
- **Quên thêm Thymeleaf** → không render được view: phải thêm `spring-boot-starter-thymeleaf`.
- **Sai H2 URL** → `/h2-console` không connect: dùng đúng URL từ `application-dev.properties`.
- **Cache Thymeleaf** ở dev gây khó reload: `spring.thymeleaf.cache=false` trong dev profile.
- **Thiếu HiddenHttpMethodFilter** nếu sau này dùng PUT/DELETE qua form: bật `spring.mvc.hiddenmethod.filter.enabled=true` ở dev.

---

## 11) Ước lượng
- T0.1–T0.3: ~0.5 ngày
- T0.4–T0.7: ~0.5 ngày

> Tổng P0: **~1 ngày** tập trung là xong, sẵn sàng bước vào P1 (Auth).


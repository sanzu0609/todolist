# Todolist Project Specification - Overview (v1)

## 1) Mục tiêu & Giá trị
- Xây dựng **Todolist đa người dùng**: mỗi user có tài khoản và danh sách task riêng.
- **Task có priority** và **subtask** (không priority).
- **Bảo mật** bằng Spring Security (form login), session-based.
- **UI SSR** bằng Thymeleaf (dơn giản, dễ triển khai).
- **Kiến trúc Layered** để code rõ ràng, dễ mở rộng, dễ test.

## 2) Phạm vi v1 (In-scope)
- Đăng ký, đăng nhập, đăng xuất (form login).
- CRUD User (tối thiểu: profile, đổi mật khẩu).
- CRUD Task (title/description/priority/status/dueDate, filter & sort cơ bản).
- CRUD Subtask (thuộc về 1 Task, không priority).
- Phân quyền theo **owner** (chỉ xử lý dữ liệu của chính mình).
- Lưu trữ bằng **H2** (file hoặc in-memory) cho dev/demo.

### Không thuộc phạm vi v1 (Out-of-scope)
- Social login (Google/GitHub), 2FA.
- Realtime, thông báo (email/WS), drag & drop.
- i18n, theming, mobile app, API public.
- Quản trị hệ thống (ADMIN) — cân nhắc v2.

## 3) Đối tượng & Persona
- **End-user (USER)**: đăng ký tài khoản, quản lý task/subtask cá nhân, theo dõi tiến độ.
- **(Tuỳ chọn v2) Admin**: giám sát hệ thống, xử lý user report.

## 4) Kiến trúc & Công nghệ
- **Layered Architecture**
  - `web` (Controller + Thymeleaf views)
  - `service` (business logic, transaction boundary)
  - `repository` (Spring Data JPA)
  - `domain` (Entity, Enum, DTO)
  - `security` (config, user details)
  - `config` (bean chung, properties)
  - `exception` (global handler)
- **Stack**: Java 21, Spring Boot 3.x, Spring MVC, Spring Data JPA, Spring Security, Thymeleaf, H2, Maven.
- **Triển khai**: chạy local (dev profile); sản phẩm demo fat-jar.

## 5) Mô hình dữ liệu (khái quát)
- **User**(id, username, passwordHash, displayName, role, createdAt, updatedAt)
- **Task**(id, owner(User), title, description, priority[LOW|MEDIUM|HIGH|URGENT], status[TODO|IN_PROGRESS|DONE|ARCHIVED], dueDate, createdAt, updatedAt, version)
- **Subtask**(id, task(Task), title, status[TODO|IN_PROGRESS|DONE], createdAt, updatedAt)

Quan hệ:
- User 1–N Task
- Task 1–N Subtask (xóa Task → cascade xoá Subtask)

## 6) Bảo mật & Quyền truy cập
- **Form login** tại `/login`; **logout** POST `/logout`.
- Trang mở: `/login`, `/register`, `/css/**`, `/js/**`.
- Trang cần đăng nhập: `/tasks/**`, `/subtasks/**`, `/profile/**`.
- Mật khẩu **BCrypt** (10–12 rounds).
- **Ownership enforcement**: mọi truy vấn/CRUD Task/Subtask đều kèm `ownerId`.
- CSRF bật mặc định; session timeout (ví dụ 30’).

## 7) Use Cases chính (v1)
1. **Đăng ký**: user tạo tài khoản, password được hash.
2. **Đăng nhập/đăng xuất**: sau login chuyển về `/tasks`.
3. **Quản lý hồ sơ**: đổi displayName, đổi mật khẩu (yêu cầu mật khẩu cũ).
4. **Task**
   - Tạo/sửa/xoá/đổi trạng thái (status), set priority & dueDate.
   - Danh sách có **filter** (status/priority/dueDate) + **sort** cơ bản.
   - (Chính sách) Khi set `DONE`, có thể **tự động** set tất cả subtasks `DONE` hoặc cảnh báo (chọn 1).
5. **Subtask**
   - Tạo/sửa/xoá, **toggle** status nhanh.
   - Chỉ chủ Task mới thao tác được.

## 8) Yêu cầu phi chức năng (NFR)
- **Hiệu năng**: danh sách task cá nhân ≤ 500 bản ghi vẫn phản hồi < 500ms local dev.
- **Bảo mật**: hash mật khẩu, không log plaintext, hạn chế lộ ID người khác (404/403).
- **Khả năng mở rộng**: dễ chuyển H2 → PostgreSQL/MySQL; dễ mở rộng REST API.
- **Khả năng test**: unit test service; integration test auth & CRUD chính.
- **Khả năng bảo trì**: coding style nhất quán, tách lớp rõ ràng, DTO cho form-binding.

## 9) Tiêu chí hoàn thành (Acceptance Criteria tổng)
- [ ] Không đăng nhập → vào `/tasks` bị redirect `/login`.
- [ ] Đăng ký thành công → có thể đăng nhập, nhìn thấy `/tasks` rỗng.
- [ ] User **chỉ** thấy/CRUD **Task/Subtask của mình**.
- [ ] CRUD Task/Subtask hoạt động, lỗi validation hiện rõ trên form.
- [ ] Xoá Task → Subtask bị xoá theo (cascade).
- [ ] Change status/toggle subtask hoạt động như kỳ vọng (theo chính sách đã chọn).
- [ ] Mật khẩu lưu dạng hash; CSRF & session chạy mặc định.
- [ ] Có ít nhất 5–8 test (unit + integration) cho flow chính.

## 10) Ràng buộc & Giả định
- Triển khai SSR, **không** xây REST public ở v1.
- Dùng session-based auth; **không** stateless JWT ở v1.
- H2 phục vụ dev/demo; có profile `dev` và `test`.
- Thời gian thực hiện v1 ước lượng: 2–4 ngày làm việc tập trung.

## 11) Rủi ro & Phương án
- **Ownership bug** → luôn dùng repo query có `ownerId` (`findByIdAndOwnerId`), test phủ.
- **Xung đột cập nhật** → dùng `@Version` cho Task (optimistic locking).
- **Form lỗi/CSRF** → Thymeleaf `<form>` chèn `_csrf` mặc định; có GlobalExceptionHandler.

## 12) Lộ trình (Roadmap)
- **P0**: Boot project, base layout Thymeleaf, cấu hình H2.
- **P1**: Auth (register/login/logout) + BCrypt + `UserDetailsService`.
- **P2**: Task (entity → repo → service → controller → view) + filter/sort + ownership.
- **P3**: Subtask (inline form, toggle nhanh).
- **P4**: Validation, flash message, error pages (403/404/500), GlobalExceptionHandler.
- **P5**: Test (unit & integration), seed data (CommandLineRunner) cho dev.

## 13) Chuẩn bị môi trường
- **Java 21**, **Maven**, **Spring Boot 3.x**
- Dev profile: H2 console (chỉ bật ở dev), test DB tách biệt.
- Cấu hình `application.properties` + `application.properties`.

## 14) Cấu trúc thư mục (gợi ý)
src/main/java/org/example/todolist/
 ├── config/             # Cấu hình chung (SecurityConfig, WebConfig)
 ├── security/           # UserDetailsService, PasswordEncoder, v.v.
 ├── domain/
 │    ├── entity/        # User, Task, Subtask
 │    ├── dto/           # Form DTO
 │    └── enums/         # Priority, Status
 ├── repository/         # Spring Data JPA Repositories
 ├── service/            # Business logic layer
 ├── web/                # Controllers (UserController, TaskController,…)
 ├── exception/          # GlobalExceptionHandler
 └── TodolistApplication.java

 src/main/resources/
 ├── templates/
 │    ├── auth/          # login.html, register.html
 │    ├── user/          # profile.html
 │    ├── task/          # list.html, form.html, detail.html
 │    └── subtask/       # _list.html, _form.html (fragment)
 ├── static/
 │    ├── css/
 │    └── js/
 └── application.properties


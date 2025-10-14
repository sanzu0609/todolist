# Spec Giai Đoạn P1 — Authentication & Authorization + User (Spring Security + Thymeleaf)

> Namespace gốc của dự án: **`org.example.todolist`**  
> Mục tiêu P1: Hoàn tất **đăng ký/đăng nhập/đăng xuất**, **trang hồ sơ cá nhân**, **bảo mật form & session**, và **khung kiểm soát quyền sở hữu (ownership)** sẵn cho P2.

---

## 0) Phạm vi & Kết quả mong đợi
**Trong phạm vi**
- Form login `/login`, form register `/register`, logout `/logout`.
- Lưu mật khẩu dạng **BCrypt**; cấu hình Spring Security với **formLogin**, **CSRF ON**, session-based.
- Trang **Profile**: xem & đổi `displayName`, đổi **mật khẩu** (yêu cầu nhập mật khẩu cũ).
- Khung **UserDetailsService** và **PasswordEncoder** sẵn dùng cho các giai đoạn sau.

**Out-of-scope P1**
- Social login, quên mật khẩu qua email, xác thực 2FA.
- REST API; phân quyền `ADMIN`.

**Definition of Done (P1)**
- Không đăng nhập → truy cập `/tasks` bị **redirect `/login`**.
- Đăng ký thành công → có thể đăng nhập; mật khẩu **được hash**.
- Đăng nhập đúng → chuyển tới `/tasks` (placeholder page hoặc page rỗng).
- `/profile` chỉ chủ tài khoản truy cập; đổi displayName & đổi password hoạt động.
- CSRF token có mặt trên các form POST; session timeout thiết lập.

---

## 1) Kiến trúc lớp & vị trí file
```
src/main/java/org/example/todolist/
  security/
    SecurityConfig.java
    AppUserDetails.java
    AppUserDetailsService.java
  domain/
    entity/
      User.java
    enums/
      Role.java
    dto/
      RegisterForm.java
      ProfileForm.java
      PasswordChangeForm.java
  repository/
    UserRepository.java
  service/
    UserService.java
  web/
    AuthController.java      # /login, /register
    ProfileController.java   # /profile
  exception/
    GlobalExceptionHandler.java  # (khung, có thể hoàn thiện ở P4)

src/main/resources/
  templates/
    _layout.html
    fragments/_flash.html
    auth/login.html
    auth/register.html
    user/profile.html
  application.properties           # base
  application-dev.properties
  application-test.properties
```

---

## 2) Data Model & DTOs
### 2.1 Entity `User`
- `Long id` (PK)
- `String username` (unique, 4–32)
- `String passwordHash` (BCrypt)
- `String displayName` (optional, 1–100)
- `Role role` (enum: `USER`, `ADMIN` — default `USER`)
- `Instant createdAt`, `Instant updatedAt`

Ràng buộc:
- `username` unique, chữ/số/._- (regex gợi ý: `^[A-Za-z0-9._-]{4,32}$`)
- `passwordHash` không null

### 2.2 Enum `Role`
```java
public enum Role { USER, ADMIN }
```

### 2.3 DTOs
- `RegisterForm { username, password, confirmPassword, displayName }`
- `ProfileForm  { displayName }`
- `PasswordChangeForm { oldPassword, newPassword, confirmNewPassword }`

**Validation**
- `RegisterForm.username` theo regex; unique check tại service.
- `RegisterForm.password` ≥ 8 ký tự, có chữ + số (tối thiểu P1).
- `confirm*` phải trùng.
- `ProfileForm.displayName` length 1–100 (optional nhưng nếu có thì phải hợp lệ).
- `PasswordChangeForm` yêu cầu `oldPassword` hợp lệ.

---

## 3) Security Configuration
### 3.1 Chính sách truy cập
- **permitAll**: `/login`, `/register`, `/css/**`, `/js/**`, static.
- **authenticated**: `/tasks/**`, `/subtasks/**`, `/profile/**` (các module sau vẫn thừa hưởng rule này).

### 3.2 Form login & logout
- `loginPage("/login")`, `defaultSuccessUrl("/tasks", true)`
- `logoutUrl("/logout")`, `logoutSuccessUrl("/login?logout")`
- Bật **CSRF** mặc định; Thymeleaf form tag phải render `_csrf`.

### 3.3 Password encoder & UserDetailsService
- `PasswordEncoder` = `BCryptPasswordEncoder(10)`.
- `AppUserDetailsService` implements `UserDetailsService#loadUserByUsername` → lấy từ `UserRepository`.
- Map `User` → `UserDetails` (`AppUserDetails`).

### 3.4 Session
- Session timeout (ví dụ) **30 phút** (cấu hình ở `server.servlet.session.timeout=30m`).
- (Tuỳ chọn) `SessionCreationPolicy.IF_REQUIRED` (mặc định cho form login).

---

## 4) Controllers & Routes
### 4.1 AuthController
- `GET /login` → view `auth/login.html` (Spring Security handle POST)
- `GET /register` → view `auth/register.html`
- `POST /register` → tạo user, hash password, redirect `/login?registered` hoặc auto-login (chọn 1)

### 4.2 ProfileController
- `GET /profile` → view `user/profile.html` (pre-fill displayName)
- `POST /profile` → cập nhật displayName (chỉ current user)
- `POST /profile/password` → đổi mật khẩu (yêu cầu nhập `oldPassword` đúng)

**Ghi chú routing**
- Mọi `POST` phải kèm CSRF token.
- Sau thao tác thành công, dùng flash attribute để báo `success`.

---

## 5) Service Layer (Business Rules)
### 5.1 UserService
- `register(RegisterForm form)`
  - validate unique username
  - encode password
  - set role=USER, timestamps
- `updateDisplayName(userId, ProfileForm form)` (chỉ chủ sở hữu)
- `changePassword(userId, PasswordChangeForm form)`
  - verify oldPassword bằng `passwordEncoder.matches`
  - encode & save new password
- `findByUsername`/`findById` (support cho Profile, Security)

**Ownership note:** P1 mới cần cho User tự quản lý mình. (P2 trở đi ta sẽ enforce ownership cho Task/Subtask.)

---

## 6) View (Thymeleaf)
### 6.1 Layout chung
- `templates/_layout.html` (header với username khi login, nav: Tasks, Profile, Logout)
- `templates/fragments/_flash.html` (hiển thị message từ RedirectAttributes)

### 6.2 Trang Auth
- `auth/login.html`: form username/password, hiển thị lỗi `?error` & `?logout`.
- `auth/register.html`: form RegisterForm + hiển thị lỗi validation/duplicated username.

### 6.3 Trang User
- `user/profile.html`: 2 form
  - Cập nhật displayName
  - Đổi mật khẩu (old/new/confirm)

**UX yêu cầu**
- Nút logout là `<form method="post" th:action="@{/logout}">` để kèm CSRF.
- Hiển thị username đang login trên header.

---

## 7) Cấu hình môi trường
- Sử dụng **`.properties`** (không dùng YAML).
- Hồ sơ cấu hình (profiles):
  - `application.properties` (base mặc định)
  - `application-dev.properties` (dành cho dev)
  - `application-test.properties` (dành cho test)
- Kích hoạt profile:
  - Chạy dev: `-Dspring.profiles.active=dev` (Maven) hoặc biến môi trường `SPRING_PROFILES_ACTIVE=dev`.

**application-dev.properties (gợi ý)**
```
spring.datasource.url=jdbc:h2:file:./data/devdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.servlet.session.timeout=30m
```

**application-test.properties (gợi ý)**
```
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

server.servlet.session.timeout=30m
```

---

## 8) Kế hoạch kiểm thử (P1)
### 8.1 Unit (Mockito) — UserService
- `register`: tạo user mới, encode password, reject username trùng.
- `changePassword`: reject khi oldPassword sai, accept khi đúng.
- `updateDisplayName`: chỉ cập nhật user hiện tại.

### 8.2 Integration (SpringBootTest + MockMvc)
- Chưa login truy cập `/tasks` → 302 về `/login`.
- Đăng ký → đăng nhập được (manual login hoặc auto-login theo lựa chọn).
- Sau login, truy cập `/profile` OK; đổi displayName thành công.
- Đổi password: oldPassword sai → validation; đúng → đăng nhập lại bằng mật khẩu mới được.

---

## 9) Phân rã Task nhỏ (cho AI Agent)
> Mỗi task nên tạo nhánh riêng, PR nhỏ, kèm checklist & screenshot nếu có.

**T1 — Scaffolding & Config**
- [x] Tạo thư mục `security/`, `domain/{entity,dto,enums}`, `repository/`, `service/`, `web/`, `exception/`.
- [x] Thêm `application-dev.properties`, `application-test.properties` (base `application.properties` đã có). Bật H2 console (dev).
- [x] Tạo `_layout.html` + `fragments/_flash.html` (layout cơ bản, header/nav, flash).

**T2 — Entity & Repository**
- [x] `User` entity + audit fields (createdAt/updatedAt) + `Role` enum.
- [x] `UserRepository extends JpaRepository<User, Long>` + `Optional<User> findByUsername(String u)`.

**T3 — Security Layer**
- [x] `PasswordEncoder` bean = BCrypt(10).
- [x] `AppUserDetails` map từ `User` → `UserDetails`.
- [x] `AppUserDetailsService implements UserDetailsService` (loadByUsername).
- [x] `SecurityConfig` (permitAll `/login`, `/register`, static; authenticated else; formLogin; logout; session timeout).

**T4 — DTO & Validation**
- [x] `RegisterForm`, `ProfileForm`, `PasswordChangeForm` (+ annotations @NotBlank, @Size, pattern, v.v.).

**T5 — UserService**
- [x] `register(form)` (unique username, encode, save)
- [x] `updateDisplayName(userId, form)`
- [x] `changePassword(userId, form)` (verify old → encode new)

**T6 — AuthController**
- [x] `GET /login`, `GET /register`, `POST /register` (flash success/errors; redirect `/login?registered`).

**T7 — ProfileController**
- [x] `GET /profile` (đổ data user hiện tại)
- [x] `POST /profile` (update displayName)
- [x] `POST /profile/password` (change password)

**T8 — Views (Thymeleaf)**
- [x] `auth/login.html`, `auth/register.html`
- [x] `user/profile.html` (2 forms)
- [x] Header hiển thị username; Logout là form POST kèm CSRF.

**T9 — Testing**
- [ ] Unit test `UserService` (register, changePassword, updateDisplayName).
- [ ] Integration test flow auth + profile với MockMvc.

**T10 — Seed & README**
- [x] `CommandLineRunner` (dev) tạo user demo (`demo/demo1234`), role USER.
- [x] README: hướng dẫn chạy, tài khoản demo, đường dẫn `/login`.

---

## 10) Acceptance Criteria chi tiết (Checklist)
- [x] `/login` hiển thị form; login sai → thông báo lỗi; logout → `?logout`.
- [x] `/register` tạo user mới; username trùng → báo lỗi; password hash trong DB (H2).
- [x] Login thành công chuyển `/tasks` (tạm thời có thể là trang placeholder).
- [x] `/profile` hiển thị và cập nhật `displayName` của user hiện tại.
- [x] `/profile/password` yêu cầu `oldPassword` đúng; đổi xong đăng nhập bằng mật khẩu mới.
- [x] Tất cả form POST có CSRF token; logout qua POST.
- [x] Session hết hạn sau 30 phút không hoạt động.

---

## 11) Ghi chú kỹ thuật
- Khi render header, lấy principal từ `SecurityContextHolder` hoặc `#httpServletRequest.userPrincipal` trong Thymeleaf.
- Nên có `@ControllerAdvice` xử lý `MethodArgumentNotValidException` để hiển thị error gọn gàng (có thể hoãn qua P4).
- Lưu ý **không log** plaintext password; bật mức log SQL thận trọng trong dev.

---

## 12) Rủi ro & Tránh lỗi thường gặp
- **Thiếu CSRF** ở form logout → 403: luôn dùng `<form method="post" th:action="@{/logout}">`.
- **Quên permitAll** `/login`, `/register` → vòng lặp redirect.
- **Không encode** mật khẩu khi seed data → login fail: luôn sử dụng `PasswordEncoder` khi seed.
- **Trùng username**: validate ở service + hiển thị message rõ ràng.

---

## 13) Ước lượng thực thi
- T1–T4: ~0.5–1 ngày
- T5–T8: ~0.5–1 ngày
- T9–T10: ~0.5 ngày

> Tổng: **1.5–2.5 ngày** cho P1 nếu tập trung.


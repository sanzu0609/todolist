# Spec Giai Đoạn P4 — Hardening (Error Handling, Ownership Guard, UX & Quality)

> Namespace: **`org.example.todolist`**  
> Phụ thuộc: P0–P3 đã hoàn thành (Auth, Domain, Web UI).

Mục tiêu P4: Làm **cứng** hệ thống trước khi viết test tổng (P5): xử lý lỗi nhất quán, chặn toàn bộ đường vòng ownership, cải thiện UX (flash/error pages), logging hợp lý, và chuẩn hoá validation messages.

---

## 0) Phạm vi & Kết quả mong đợi
**Trong phạm vi**
- `GlobalExceptionHandler` cho 403/404/409/422/500.
- Exception chuẩn hoá: `EntityNotFoundException`, `AccessDeniedBusinessException`, `BusinessException`, `OptimisticLockingException` (wrap từ JPA), `ValidationException` (bind).
- Double-check **ownership** tại Service & Repository (không dựa vào UI).
- Trang lỗi Thymeleaf: `error/403.html`, `error/404.html`, `error/409.html`, `error/422.html`, `error/500.html`.
- UX: thông báo (flash), giữ giá trị form khi lỗi, confirm xoá, hiển thị badge/format ngày nhất quán.
- Logging: mức `WARN`/`ERROR` cho lỗi, ẩn thông tin nhạy cảm.
- I18n validation messages (tối thiểu file `messages.properties`).

**Definition of Done (P4)**
- Truy cập tài nguyên **không thuộc owner** ở mọi route ⇒ 404 hoặc 403 theo chính sách.
- Update Task với `version` sai ⇒ 409 (Conflict) + message hướng dẫn reload form.
- Lỗi validation form ⇒ 422 (Unprocessable Entity) (render lại form với lỗi).
- Có trang 403/404/409/422/500 đẹp, đồng bộ layout.
- Log lỗi đúng mức, không rò rỉ dữ liệu nhạy cảm.

---

## 1) Exception Types & Quy ước
`src/main/java/org/example/todolist/exception/`
- `EntityNotFoundException extends RuntimeException`  
  Sử dụng khi không tìm thấy tài nguyên theo `ownerId`.
- `AccessDeniedBusinessException extends RuntimeException`  
  Sử dụng khi tìm thấy tài nguyên nhưng user không có quyền (ít dùng vì ta filter theo owner từ đầu → ưu tiên 404 để tránh lộ thông tin).
- `BusinessException extends RuntimeException`  
  Cho các vi phạm nghiệp vụ (ví dụ policy Guard khi còn subtask chưa DONE).
- `OptimisticLockingAppException extends RuntimeException`  
  Wrap `ObjectOptimisticLockingFailureException`/`OptimisticLockException`.

**Quy ước trả mã lỗi**
- `EntityNotFoundException` ⇒ **404**
- `AccessDeniedBusinessException` ⇒ **403**
- `BusinessException` ⇒ **422** (render form hiện lỗi)
- `OptimisticLockingAppException` ⇒ **409** (render form hướng dẫn reload)
- `MethodArgumentNotValidException`/`BindException` ⇒ **422**
- Bất ngờ khác ⇒ **500**

---

## 2) Global Exception Handler
`src/main/java/org/example/todolist/exception/GlobalExceptionHandler.java`
- `@ControllerAdvice`
- `@ExceptionHandler` cho từng loại trên, add flash/message hoặc model attribute phù hợp.
- Sử dụng view lỗi dưới `templates/error/*.html` (kế thừa `_layout.html`).
- Gán HTTP status tương ứng bằng `@ResponseStatus` hoặc `ResponseEntity`.

**Ví dụ mapping** (pseudo):
- `@ExceptionHandler(EntityNotFoundException)` → return view `error/404` (status 404)
- `@ExceptionHandler(AccessDeniedBusinessException)` → view `error/403`
- `@ExceptionHandler(BusinessException)` → view `error/422` cùng message
- `@ExceptionHandler(OptimisticLockingAppException)` → view `error/409`
- `@ExceptionHandler(Exception)` → view `error/500`

**Ghi chú**
- Với lỗi validation từ form, **không** redirect sang trang lỗi; thay vào đó, quay lại form cùng `BindingResult` (đã làm ở P3). Trường hợp văng ra global (ví dụ custom binding), map sang 422.

---

## 3) Ownership Guard (Service & Repository)
- **Repository**: chỉ expose phương thức có `ownerId` trong điều kiện (`findByIdAndOwnerId`, JPQL join owner). Tránh `findById` trần trong code nghiệp vụ.
- **Service**: all public methods nhận `ownerId` (lấy từ SecurityContext), dùng finder `...AndOwnerId`, nếu null ⇒ `EntityNotFoundException`.
- **Controller**: không nhận `ownerId` từ client; lấy từ user hiện tại.
- **UI**: không render action cho resource không thuộc owner (vệ sinh UI), nhưng **không** phụ thuộc vào UI để bảo vệ quyền.

---

## 4) Error Pages & UX
`src/main/resources/templates/error/`
- `403.html` — Bạn không có quyền truy cập.
- `404.html` — Không tìm thấy tài nguyên hoặc không tồn tại.
- `409.html` — Dữ liệu vừa bị thay đổi (xung đột), vui lòng tải lại và thử lại.
- `422.html` — Dữ liệu chưa hợp lệ, vui lòng kiểm tra các trường được đánh dấu.
- `500.html` — Lỗi hệ thống, vui lòng thử lại sau.

**Yêu cầu giao diện**
- Kế thừa `_layout.html`, show biểu tượng (icon), mô tả ngắn, link quay lại trang trước (JS `history.back()`), và link về `/tasks`.
- Với `409`, đề xuất: show thông tin `version` hiện tại & nút “Reload form”.

---

## 5) Validation Messages & I18n
`src/main/resources/messages.properties`
- Chuẩn hoá thông báo lỗi cho field: `NotBlank.task.title=Tiêu đề không được để trống`, …
- Lỗi nghiệp vụ: `business.task.guard=Task còn subtask chưa hoàn thành.`
- Bật message source trong Spring Boot (Boot tự cấu hình theo `messages.properties`).

**Form hiển thị**
- `th:errors` tại mỗi field; show toàn cục (top) khi có `BusinessException`.

---

## 6) Logging & Security Headers
- Logging (application-dev.properties):
  - `logging.level.org.springframework.web=INFO`
  - `logging.level.org.hibernate.SQL=INFO` (dev khi cần)
  - Không log password / thông tin nhạy cảm.
- (Tuỳ chọn) Thêm security headers cơ bản qua `SecurityConfig` hoặc `WebConfig` (cache-control cho error pages, X-Content-Type-Options, X-Frame-Options deny trừ `/h2-console`).
- Cho `/h2-console` (dev) bật frame: cấu hình trong `SecurityConfig` (đã làm ở P1 nếu cần).

---

## 7) Ràng buộc HTTP Status trong Controller
- Với form POST invalid: trả lại **200** + form cùng `BindingResult` (không dùng 422).  
  *Tuy nhiên,* nếu ném `BusinessException`/`ValidationException` ở tầng service ⇒ Global handler trả **422**.
- Với xung đột version trong service ⇒ ném `OptimisticLockingAppException` ⇒ **409**.

> Mục tiêu là **nhất quán**, dễ test, và thân thiện người dùng SSR.

---

## 8) Phân rã Task nhỏ (cho AI Agent)
**T4.1 — Exceptions**
- [ ] Tạo các class: `EntityNotFoundException`, `AccessDeniedBusinessException`, `BusinessException`, `OptimisticLockingAppException`.

**T4.2 — GlobalExceptionHandler**
- [ ] Implement `@ControllerAdvice` map lỗi → view + status.
- [ ] Chuẩn hoá thông điệp (Model attrs) & flash khi redirect.

**T4.3 — Ownership Audit**
- [ ] Rà soát repo/service, thay tất cả `findById` trần bằng phiên bản có `ownerId`.
- [ ] Thêm unit test nhỏ mô phỏng truy cập chéo user ⇒ 404/403.

**T4.4 — Error Pages**
- [ ] Tạo `templates/error/{403,404,409,422,500}.html` theo layout.

**T4.5 — Validation Messages**
- [ ] Tạo `messages.properties` với thông báo field & business.
- [ ] Chỉnh view form hiển thị `th:errors` đúng chỗ.

**T4.6 — Logging & Headers**
- [ ] Cấu hình logging mức INFO/WARN/ERROR hợp lý.
- [ ] (Tuỳ chọn) Security headers; bật frame cho `/h2-console` ở dev.

---

## 9) Acceptance Criteria chi tiết (Checklist)
- [ ] Truy cập task/subtask của user khác bằng URL tay ⇒ nhận **404** (hoặc 403 theo chính sách) với trang lỗi chuẩn.
- [ ] Update Task với `version` cũ ⇒ **409** + thông điệp rõ ràng; reload form sửa được.
- [ ] Lỗi nghiệp vụ (policy Guard) ⇒ **422** với thông điệp dễ hiểu, giữ giá trị form.
- [ ] Trang 403/404/409/422/500 có layout thống nhất, có nút quay lại/đi tới `/tasks`.
- [ ] Log lỗi đúng mức, không rò rỉ dữ liệu nhạy cảm.

---

## 10) Ước lượng
- T4.1–T4.3: ~0.5 ngày
- T4.4–T4.6: ~0.5 ngày

> Tổng P4: **~1 ngày** tập trung.


# Spec Giai Đoạn P5 — Testing, Seed & Packaging

> Namespace: **`org.example.todolist`**  
> Phụ thuộc: P0–P4 đã hoàn thành.

Mục tiêu P5: Bổ sung **seed data** cho môi trường dev, hoàn thiện **test tầng Service & Integration (MockMvc + Security)**, thiết lập **coverage mục tiêu**, chuẩn hoá **build/package** và cập nhật **README** để demo/phỏng vấn.

---

## 0) Phạm vi & Kết quả mong đợi
**Trong phạm vi**
- `CommandLineRunner` seed dữ liệu dev (user demo + task/subtask mẫu).
- **Unit tests** cho Service chính (User/Task/Subtask).
- **Integration tests** dùng `@SpringBootTest` + `@AutoConfigureMockMvc` + H2 in-memory.
- Thiết lập mục tiêu **coverage ~70%** cho services và logic nghiệp vụ trọng yếu.
- **Build** ra jar chạy được, hướng dẫn profile & lệnh trong README.

**Definition of Done (P5)**
- `mvn -Pdev spring-boot:run` (hoặc `-Dspring-boot.run.profiles=dev`) chạy có **seed data**.
- `mvn -Ptest test` pass tất cả unit + integration tests.
- `mvn package` sinh `todolist-*.jar`; chạy bằng `java -jar` OK với profile `dev`.
- README có hướng dẫn chạy + tài khoản demo + hình UI (nếu có).

---

## 1) Seed Data (dev profile)
`src/main/java/org/example/todolist/config/DevDataSeeder.java`
- Annotate `@Profile("dev")` + `@Configuration` hoặc `@Component` + `CommandLineRunner`.
- Dùng `PasswordEncoder` để encode mật khẩu.

**Dữ liệu đề xuất**
- Users:
  - `demo / demo1234` (ROLE_USER)
  - `alice / alice1234` (ROLE_USER)
- Tasks/Subtasks mẫu cho mỗi user:
  - Task 1: `"Học Spring"` (priority HIGH, status IN_PROGRESS, due +3 days)
    - Subtask: `"Đọc về DI"` (DONE)
    - Subtask: `"Viết demo @Service"` (TODO)
  - Task 2: `"Dọn CV"` (priority MEDIUM, status TODO, due +7 days)
    - Subtask: `"Cập nhật dự án Todo"` (TODO)

**Chú ý**
- Check tồn tại trước khi seed (tránh duplicate mỗi lần restart): `userRepository.findByUsername("demo").isEmpty()`.

---

## 2) Unit Tests (Mockito) — Service Layer
`src/test/java/org/example/todolist/service/`

### 2.1 UserServiceTest
- `register_success_encodesPassword_andAssignsRoleUser`.
- `register_rejects_duplicateUsername`.
- `changePassword_rejects_whenOldPasswordWrong`.
- `changePassword_success_updatesHash`.

### 2.2 TaskServiceTest
- `create_setsOwner_andDefaultStatusTODO`.
- `list_filtersByOwner_andAppliesFilterAndSort` (mock repo `Page<Task>`).
- `update_rejects_whenNotOwner` (findByIdAndOwnerId returns empty).
- `changeStatus_toDone_appliesPolicy` (auto-complete hoặc guard) — mock `subtaskRepository`.
- `delete_cascadeSubtasks` (verify repo interactions).

### 2.3 SubtaskServiceTest
- `create_rejects_whenTaskNotOwnedByUser`.
- `toggleStatus_cyclesOrSwitchesCorrectly`.
- `update_respectsOwnership`.
- `delete_respectsOwnership`.

**Kỹ thuật**
- Sử dụng `@ExtendWith(MockitoExtension.class)`.
- Mock repository, inject service (`@InjectMocks`).
- Dùng `ArgumentCaptor` khi cần kiểm tra entity được lưu.

---

## 3) Integration Tests — MockMvc + Security + H2
`src/test/java/org/example/todolist/integration/`

**Thiết lập**
- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- Sử dụng `application-test.properties` (H2 in-memory, `ddl-auto=create-drop`).
- Seed data test riêng (bằng SQL hoặc `@TestConfiguration` + Runner) — **không dùng DevDataSeeder** trong test.

### 3.1 AuthFlowIT
- `whenNotLoggedIn_accessTasks_redirectsToLogin` (GET `/tasks` → 302 `/login`).
- `login_withValidUser_redirectsToTasks` (POST form `/login`).
- `logout_clearsSession`.

### 3.2 TaskCrudIT
- `list_showsOnlyCurrentUsersTasks` (login as `demo`, ensure không thấy của `alice`).
- `createTask_valid_returnsRedirectAndVisibleInList` (POST `/tasks`).
- `updateTask_withValidVersion_updates`.
- `updateTask_withStaleVersion_returns409` (mô phỏng stale bằng cách lưu version cũ trước khi submit).
- `deleteTask_removesItsSubtasks`.

### 3.3 SubtaskFlowIT
- `createSubtask_forOwnTask_ok`.
- `toggleSubtaskStatus_ok`.
- `accessSubtask_ofOtherUser_returns404`.

**Kỹ thuật Security**
- Dùng `SecurityMockMvcRequestPostProcessors`:
  - `.with(csrf())` cho POST.
  - `.with(user("demo").roles("USER"))` hoặc flow login thực thụ POST `/login`.
- Hoặc dùng `@WithMockUser(username = "demo", roles = "USER")` (khi bypass login cho test controller).

---

## 4) Coverage & Báo cáo
- Mục tiêu khoảng **70%** cho services & logic nghiệp vụ (không cứng nhắc nhưng nên đạt được).
- Có thể dùng plugin `jacoco-maven-plugin` để tạo report (tuỳ chọn P5).

**Gợi ý cấu hình Jacoco (tuỳ chọn)**
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Báo cáo sẽ nằm trong `target/site/jacoco/index.html`.

---

## 5) Packaging & Chạy
**Build**
- `mvn clean package`

**Run (dev profile)**
- `java -jar target/todolist-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev`

**Run (test profile)**
- Chạy test tự động qua Maven: `mvn test` (sẽ pick `application-test.properties`).

**Ports/Console**
- App mặc định port 8080; H2 console `/h2-console` (dev).

---

## 6) README cập nhật
Thêm các mục:
- **Giới thiệu** ngắn (mục tiêu dự án, stack).
- **Cách chạy**:
  - Dev: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
  - Jar: lệnh run kèm profile.
- **Tài khoản demo**: `demo/demo1234`.
- **Ảnh chụp UI**: `task list`, `task detail` với subtask panel.
- **Kiến trúc & Packages**: sơ đồ đơn giản.
- **Testing**: cách chạy test, Jacoco (nếu bật).

---

## 7) Phân rã Task nhỏ (cho AI Agent)
**T5.1 — DevDataSeeder**
- [x] Tạo `DevDataSeeder` (profile `dev`) seed users/tasks/subtasks.

**T5.2 — Unit Tests (Service)**
- [x] Viết `UserServiceTest`.
- [x] Viết `TaskServiceTest`.
- [x] Viết `SubtaskServiceTest`.

**T5.3 — Integration Tests (MockMvc)**
- [x] `AuthFlowIT`.
- [x] `TaskCrudIT`.
- [x] `SubtaskFlowIT`.

**T5.4 — Jacoco (tùy chọn)**
- [x] Thêm plugin jacoco, tạo report.

**T5.5 — Packaging & README**
- [x] `mvn package` và thử chạy jar với `--spring.profiles.active=dev`.
- [x] Cập nhật README với hướng dẫn chạy + ảnh UI.

---

## 8) Acceptance Criteria chi tiết (Checklist)
- [ ] Seed dev tạo thành công `demo`, `alice` và data mẫu (không bị duplicate mỗi lần chạy).
- [ ] All unit tests pass; bao phủ các rule: ownership, status-change policy, optimistic locking, validation.
- [ ] All integration tests pass; luồng đăng nhập/CRUD/ownership hoạt động.
- [x] `mvn package` sinh jar chạy được; start app với profile `dev` OK.
- [x] README đầy đủ, người mới clone repo chạy được trong 5 phút.

---

## 9) Rủi ro & Lưu ý
- **Seed dùng password plain** → login fail: luôn dùng `PasswordEncoder`.
- **Dùng DevDataSeeder trong test** → test lệ thuộc profile dev: tách seed test riêng.
- **Thiếu CSRF trong MockMvc POST** → 403: nhớ `.with(csrf())`.
- **Phụ thuộc giờ hệ thống (dueDate)**: khi test, tạo dữ liệu với `LocalDate.now()` nhưng lưu ý timezone.

---

## 10) Ước lượng
- T5.1–T5.3: ~0.75–1.0 ngày
- T5.4–T5.5: ~0.25–0.5 ngày

> Tổng P5: **~1.0–1.5 ngày** tập trung.

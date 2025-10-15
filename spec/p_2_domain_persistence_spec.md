# Spec Giai Đoạn P2 — Domain & Persistence (Task/Subtask)

> Namespace: **`org.example.todolist`**  
> Mục tiêu P2: Xây mô hình dữ liệu **User–Task–Subtask**, định nghĩa **enums**, ánh xạ **JPA** với **ownership** theo `ownerId`, tối ưu quan hệ & ràng buộc, và hoàn thiện **Repository + Service skeleton** (chưa làm UI/Controller – sẽ sang P3).

---

## 0) Phạm vi & Kết quả mong đợi
**Trong phạm vi**
- Khai báo **Entity**: `Task`, `Subtask` (P1 đã có `User`).
- Khai báo **Enum**: `Priority`, `TaskStatus`, `SubtaskStatus`.
- Cấu hình quan hệ: `User 1-N Task`, `Task 1-N Subtask` (delete Task ⇒ cascade Subtasks).
- **Ownership**: mọi truy cập Task/Subtask phải ràng buộc theo `owner(user.id)`.
- **Repository**: phương thức CRUD + các finder theo owner/filter cơ bản.
- **Service skeleton**: nghiệp vụ chính (create/update/delete/change status), `@Transactional`, kiểm tra ownership.
- **Validation**: annotation cơ bản (title non-blank, enum required, v.v.).

**Definition of Done (P2)**
- Chạy dev với H2, JPA tự tạo schema; tạo/lưu/xoá Task/Subtask thành công.
- Repository trả dữ liệu **đúng owner**; không thể truy cập tài nguyên người khác qua repo/service.
- Xoá Task ⇒ Subtasks bị xoá theo.
- Optimistic Locking hoạt động trên `Task` (tránh ghi đè ngoài ý muốn).

---

## 1) Enums
`src/main/java/org/example/todolist/domain/enums/`
```java
public enum Priority { LOW, MEDIUM, HIGH, URGENT }
public enum TaskStatus { TODO, IN_PROGRESS, DONE, ARCHIVED }
public enum SubtaskStatus { TODO, IN_PROGRESS, DONE }
```

---

## 2) Entities
`src/main/java/org/example/todolist/domain/entity/`

### 2.1 Task
- **Trường**
  - `Long id` (PK)
  - `User owner` — `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "owner_id", nullable=false)`
  - `String title` — required, max 255
  - `String description` — `@Lob` (optional)
  - `Priority priority` — required
  - `TaskStatus status` — default `TODO`
  - `LocalDate dueDate` — optional
  - `Instant createdAt`, `Instant updatedAt`
  - `Long version` — `@Version` (optimistic locking)
  - `List<Subtask> subtasks` — `@OneToMany(mappedBy="task", cascade = CascadeType.REMOVE, orphanRemoval = true)`

- **Ràng buộc & Index**
  - `@Table(indexes = { @Index(name="idx_task_owner", columnList="owner_id"), @Index(name="idx_task_due", columnList="due_date"), @Index(name="idx_task_priority", columnList="priority"), @Index(name="idx_task_status", columnList="status") })`

- **Validation**
  - `@NotBlank` `title`
  - `@NotNull` `priority`

### 2.2 Subtask
- **Trường**
  - `Long id` (PK)
  - `Task task` — `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "task_id", nullable=false)`
  - `String title` — required, max 255
  - `SubtaskStatus status` — default `TODO`
  - `Instant createdAt`, `Instant updatedAt`

- **Ràng buộc & Index**
  - `@Table(indexes = { @Index(name="idx_subtask_task", columnList="task_id") })`

- **Validation**
  - `@NotBlank` `title`

**Ghi chú**
- Subtask **không có priority** (theo yêu cầu).
- Chính sách xoá: **xoá Task ⇒ xoá mọi Subtask** (`CascadeType.REMOVE` + `orphanRemoval=true`).

---

## 3) Auditing (đề xuất)
- Dùng listener đơn giản ở entity hoặc `@PrePersist/@PreUpdate` để set `createdAt/updatedAt`.
- (Tuỳ chọn P4) Bật Spring Data JPA Auditing để có AuditorAware (không bắt buộc P2).

---

## 4) Repositories
`src/main/java/org/example/todolist/repository/`

### 4.1 TaskRepository
```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    // List theo owner
    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    // Filter cơ bản (kết hợp owner)
    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);
    Page<Task> findByOwnerIdAndPriority(Long ownerId, Priority priority, Pageable pageable);
    Page<Task> findByOwnerIdAndDueDateLessThanEqual(Long ownerId, LocalDate dueDate, Pageable pageable);

    // Search theo tiêu đề (LIKE) + owner
    @Query("select t from Task t where t.owner.id=:ownerId and lower(t.title) like lower(concat('%', :q, '%'))")
    Page<Task> searchByTitle(@Param("ownerId") Long ownerId, @Param("q") String q, Pageable pageable);
}
```

### 4.2 SubtaskRepository
```java
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {
    // Lấy subtask theo id nhưng ràng buộc task.owner.id = ownerId
    @Query("select s from Subtask s where s.id=:id and s.task.owner.id=:ownerId")
    Optional<Subtask> findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    // Lấy theo task (đảm bảo task thuộc owner)
    @Query("select s from Subtask s where s.task.id=:taskId and s.task.owner.id=:ownerId")
    List<Subtask> findByTaskIdAndOwnerId(@Param("taskId") Long taskId, @Param("ownerId") Long ownerId);
}
```

**Nguyên tắc repo**
- Mọi finder expose ra **phải** có biến `ownerId` trong điều kiện (trực tiếp hoặc qua join).
- Tránh dùng `findById(id)` trần trong service cho tài nguyên thuộc user.

---

## 5) Service Layer (skeleton)
`src/main/java/org/example/todolist/service/`

### 5.1 TaskService
Chịu trách nhiệm nghiệp vụ cho Task, **luôn kiểm tra ownership**.

**Methods (gợi ý)**
```java
public interface TaskService {
    Page<Task> list(Long ownerId, TaskFilter filter, Pageable pageable);
    Task create(Long ownerId, TaskCreateDto dto);
    Task update(Long ownerId, Long taskId, TaskUpdateDto dto);
    Task changeStatus(Long ownerId, Long taskId, TaskStatus newStatus);
    void delete(Long ownerId, Long taskId);
    Task getOrThrow(Long ownerId, Long taskId);
}
```

**Business Rules**
- `create`:
  - set `owner`, default `status=TODO` nếu dto không đưa vào.
  - validate `dueDate` (tuỳ chọn: không < hôm nay).
- `update`:
  - chỉ cho phép owner; dùng `@Transactional`.
  - dùng `@Version` của entity để tránh lost update.
- `changeStatus`:
  - nếu chuyển sang `DONE`, **chọn 1 policy** (quyết định ở P3 UI):
    1) **Auto-complete**: set ALL subtasks`=DONE`.
    2) **Guard**: nếu còn subtask chưa DONE ⇒ reject.
- `delete`:
  - xoá Task ⇒ subtasks bị xoá theo; kiểm tra owner trước khi xoá.

### 5.2 SubtaskService
**Methods (gợi ý)**
```java
public interface SubtaskService {
    List<Subtask> listByTask(Long ownerId, Long taskId);
    Subtask create(Long ownerId, Long taskId, SubtaskCreateDto dto);
    Subtask update(Long ownerId, Long taskId, Long subtaskId, SubtaskUpdateDto dto);
    Subtask toggleStatus(Long ownerId, Long taskId, Long subtaskId);
    void delete(Long ownerId, Long taskId, Long subtaskId);
}
```

**Business Rules**
- `create`/`update`:
  - task phải thuộc owner; `title` non-blank.
- `toggleStatus`:
  - đổi `TODO ↔ IN_PROGRESS ↔ DONE` (định nghĩa rõ trong P3; P2 có thể để `TODO → DONE → TODO` theo nhu cầu).

**Ghi chú kỹ thuật**
- Các service triển khai: `@Service` + `@Transactional` (readOnly cho list/get, writable cho create/update/delete).
- Chỉ dùng repo methods có ràng buộc owner (không dùng phương thức trần).
- Nên tách DTO (create/update/filter) ở package `domain/dto`.

---

## 6) DTOs (gợi ý)
`src/main/java/org/example/todolist/domain/dto/`

```java
public record TaskCreateDto(String title, String description, Priority priority, LocalDate dueDate) {}
public record TaskUpdateDto(String title, String description, Priority priority, LocalDate dueDate, TaskStatus status, Long version) {}

public record TaskFilter(TaskStatus status, Priority priority, LocalDate dueOnOrBefore, String q) {}

public record SubtaskCreateDto(String title) {}
public record SubtaskUpdateDto(String title, SubtaskStatus status) {}
```

> Lưu ý: `TaskUpdateDto` có `version` để client gửi lên nhằm tránh lost update (P3 sẽ bind từ form hidden field).

---

## 7) Validation & Error Handling
- Dùng `jakarta.validation` annotations trên DTO: `@NotBlank`, `@NotNull`, `@Size`…
- Service: nếu không tìm thấy theo `ownerId` ⇒ ném `EntityNotFoundException` (custom) để GlobalExceptionHandler xử lý 404 ở P4.
- Nếu vi phạm policy (ví dụ Guard khi còn subtask chưa DONE) ⇒ ném `BusinessException` (custom) – P3 hiển thị message.

---

## 8) H2/JPA note (properties)
- `spring.jpa.hibernate.ddl-auto=update` (dev) sẽ tự tạo bảng/alter khi thêm entity.
- Kiểm tra `/h2-console` và xác nhận bảng `tasks`, `subtasks` sinh ra đúng.

---

## 9) Phân rã Task nhỏ (cho AI Agent)
**T2.1 — Enums**
- [x] Tạo `Priority`, `TaskStatus`, `SubtaskStatus` trong `domain/enums`.

**T2.2 — Entities**
- [x] Tạo `Task` với các trường & mapping, `@Version`, indexes.
- [x] Tạo `Subtask` với mapping về `Task`, indexes.
- [x] Thêm `@PrePersist/@PreUpdate` set `createdAt/updatedAt` (cho cả 2 entity).

**T2.3 — Repositories**
- [x] `TaskRepository` với finder theo `ownerId` + filter + search.
- [x] `SubtaskRepository` với truy vấn ràng buộc `task.owner.id`.

**T2.4 — DTOs**
- [ ] Tạo DTO: `TaskCreateDto`, `TaskUpdateDto`, `TaskFilter`, `SubtaskCreateDto`, `SubtaskUpdateDto` + validation annotations.

**T2.5 — Services (interfaces + impl skeleton)**
- [ ] `TaskService` + `TaskServiceImpl` (create/update/changeStatus/delete/getOrThrow/list).
- [ ] `SubtaskService` + `SubtaskServiceImpl` (listByTask/create/update/toggle/delete).
- [ ] Tất cả method kiểm tra **ownership** bằng repo finder có `ownerId`.

**T2.6 — Smoke test bằng CommandLineRunner (tạm thời dev)**
- [ ] (Tuỳ chọn) Tạo vài `Task`/`Subtask` mẫu cho user demo (đã seed ở P1) để kiểm tra mapping & cascade.

**T2.7 — Unit tests (Mockito)**
- [ ] `TaskService`: create (set owner, default status), update (owner-only), changeStatus (auto-complete hoặc guard — stub), delete (cascade), list(filter).
- [ ] `SubtaskService`: create/update/toggle/delete với ownership check.

---

## 10) Acceptance Criteria chi tiết (Checklist)
- [ ] JPA sinh bảng `tasks` và `subtasks` đúng cột & FK; xoá Task xoá luôn Subtask.
- [ ] `TaskRepository.findByIdAndOwnerId` và các finder theo owner chạy đúng; không bao giờ trả về tài nguyên không thuộc owner.
- [ ] `SubtaskRepository.findByIdAndOwnerId` & `findByTaskIdAndOwnerId` hoạt động.
- [ ] `TaskService`/`SubtaskService` bắt buộc ownership, `@Transactional` đúng chỗ.
- [ ] `Task` có `@Version` và xử lý được xung đột (sẽ test UI ở P3).

---

## 11) Rủi ro & Lưu ý
- **Quên ràng buộc owner** trong repo ⇒ lộ dữ liệu: mọi truy vấn phải có `ownerId`.
- **Thiếu cascade/orphanRemoval** ⇒ Subtask mồ côi: bật trên quan hệ `Task.subtasks`.
- **Lạm dụng `findById(id)`** ở service ⇒ bypass ownership: luôn dùng `findByIdAndOwnerId`/JPQL có owner.
- **Không truyền `version` khi update** ⇒ có thể lost update: thêm field `version` trong form (P3) và DTO update.

---

## 12) Ước lượng
- T2.1–T2.3: ~0.5 ngày
- T2.4–T2.5: ~0.5–0.75 ngày
- T2.6–T2.7: ~0.25–0.5 ngày

> Tổng P2: **~1.25–1.75 ngày** tập trung.


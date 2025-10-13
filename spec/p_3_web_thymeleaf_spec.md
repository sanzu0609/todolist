# Spec Giai Đoạn P3 — Web (Thymeleaf) cho Task/Subtask

> Namespace: **`org.example.todolist`**  
> Phụ thuộc: P0 (layout/config), P1 (Auth + Profile), P2 (Entity/Repo/Service).

Mục tiêu P3: Hoàn thiện **UI SSR** bằng Thymeleaf cho **Task** và **Subtask**: danh sách + filter/sort/search, tạo/sửa/xoá, đổi trạng thái nhanh, và hiển thị/validate lỗi rõ ràng. Tất cả thao tác phải **ràng buộc ownership** (chỉ dữ liệu của current user).

---

## 0) Phạm vi & Kết quả mong đợi
**Trong phạm vi**
- Controller + View Thymeleaf cho Task/Subtask.
- Danh sách Task có **filter** (status/priority/dueDate) + **search title** + **sort** cơ bản.
- Tạo/Sửa/Xoá Task; **đổi trạng thái nhanh** từ list.
- Trang chi tiết Task + panel Subtask: tạo/sửa/xoá, **toggle status** nhanh.
- Flash message & hiển thị lỗi validation (`@Valid` + `BindingResult`).

**Out-of-scope P3**
- REST API public, Ajax/HTMX, realtime.

**Definition of Done (P3)**
- UI Task/Subtask chạy end-to-end; ownership enforced.
- Filter/sort/search hoạt động; đổi trạng thái nhanh ok.
- Xoá Task ⇒ Subtasks xoá theo; lỗi form hiển thị đúng.

---

## 1) Cấu trúc thư mục View
`src/main/resources/templates/`
```
_layout.html
fragments/_flash.html
fragments/_pagination.html         # (tuỳ chọn nếu dùng Page)
auth/ (P1)
user/ (P1)

task/
  list.html
  form.html        # dùng chung create/edit
  detail.html      # hiển thị task + panel subtask

subtask/
  _list.html       # fragment được include trong task/detail
  _form.html       # fragment inline add/edit
```

---

## 2) Routing & Controllers
`src/main/java/org/example/todolist/web/`

### 2.1 TaskController
**Routes**
- `GET  /tasks` → list (filter + search + sort + page)
- `GET  /tasks/new` → form create
- `POST /tasks` → create
- `GET  /tasks/{id}` → detail
- `GET  /tasks/{id}/edit` → form edit
- `POST /tasks/{id}` → update (SSR dùng POST thay cho PUT)
- `POST /tasks/{id}/status` → change status nhanh
- `POST /tasks/{id}/delete` → delete (SSR dùng POST thay cho DELETE)

**Lưu ý**
- **Ownership**: mọi thao tác lấy `{id}` đều qua service `getOrThrow(ownerId, id)`; list luôn filter theo `ownerId`.
- Form POST phải có CSRF.

### 2.2 SubtaskController
**Routes** (nằm dưới Task)
- `POST /tasks/{taskId}/subtasks` → create
- `POST /tasks/{taskId}/subtasks/{id}` → update
- `POST /tasks/{taskId}/subtasks/{id}/status` → toggle status nhanh
- `POST /tasks/{taskId}/subtasks/{id}/delete` → delete

**Lưu ý**
- Luôn truyền `ownerId` hiện tại xuống service; service xác minh `task.owner.id`.
- Sau thao tác, redirect về `/tasks/{taskId}` với flash message.

---

## 3) Binding & DTOs (reuse từ P2)
- **TaskCreateDto**: `title, description, priority, dueDate`  (validate: `@NotBlank title`, `@NotNull priority`)
- **TaskUpdateDto**: `title, description, priority, dueDate, status, version`
- **TaskFilter**: `status, priority, dueOnOrBefore, q`
- **SubtaskCreateDto**: `title`
- **SubtaskUpdateDto**: `title, status`

**Form mapping**
- Thymeleaf `th:object` + field `th:field="*{...}"`; hidden field cho `version` khi edit Task.

---

## 4) Chính sách Status & Ownership
- **Task.changeStatus**: chọn một trong hai policy (quyết định ở P3, default **Auto-complete**):
  1) **Auto-complete**: khi Task chuyển `DONE` ⇒ set toàn bộ Subtask `DONE`.
  2) **Guard**: nếu còn subtask chưa `DONE` ⇒ reject với message.
- **Ownership**: không render link/action nếu không thuộc owner (dù controller/service đã chặn, UI vẫn nên ẩn cho sạch).

---

## 5) List Tasks (GET /tasks)
**Input query params**
- `status` ∈ {`TODO`,`IN_PROGRESS`,`DONE`,`ARCHIVED`} (optional)
- `priority` ∈ {`LOW`,`MEDIUM`,`HIGH`,`URGENT`} (optional)
- `dueOnOrBefore` (yyyy-MM-dd) (optional)
- `q` (search theo title, optional)
- `sort` (vd: `dueDate,asc` | `priority,desc` | `createdAt,desc`)
- `page`, `size` (tuỳ chọn dùng `Pageable`)

**Controller**
- Map params → `TaskFilter` + `Pageable`.
- Gọi `taskService.list(ownerId, filter, pageable)`.

**View `task/list.html`**
- Form filter (select status/priority, input date, search q).
- Bảng danh sách: title, priority (badge), status (badge), dueDate, actions (View, Edit, Delete, Change Status quick dropdown).
- (Tuỳ chọn) include fragment `_pagination.html`.

---

## 6) Create/Edit Task
**Create**
- `GET /tasks/new` → `task/form.html` với `th:object` rỗng.
- `POST /tasks` → bind `TaskCreateDto` + `@Valid` → service `create` → redirect `/tasks` (flash `Created`).

**Edit**
- `GET /tasks/{id}/edit` → load Task → map sang form model (bao gồm `version`).
- `POST /tasks/{id}` → bind `TaskUpdateDto` (+ hidden `version`) → `service.update` → redirect `/tasks/{id}` (flash `Updated`).

**Validation errors**
- Trả về `task/form.html` cùng BindingResult, hiển thị `th:errors` tại từng field.

---

## 7) Detail Task & Subtasks Panel (GET /tasks/{id})
**View `task/detail.html`**
- Hiển thị thông tin Task (title, description, priority, status, dueDate, updatedAt).
- Nút **Change Status** nhanh cho Task (form POST `/tasks/{id}/status`).
- Include fragment `subtask/_list.html` hiển thị các subtask:
  - Mỗi subtask: title + badge status + actions: **Toggle**, **Edit**, **Delete**.
- Form thêm Subtask inline (include `subtask/_form.html`): POST `/tasks/{taskId}/subtasks`.

**Subtask Edit**
- Cách 1: Modal (nếu muốn nâng UI) — ngoài phạm vi P3 cơ bản.
- Cách 2: Điều hướng sang `#edit-{subtaskId}` trong cùng page, render form inline.

---

## 8) Delete & Confirm
- Delete Task: POST `/tasks/{id}/delete` → redirect `/tasks` (flash `Deleted`).
- Delete Subtask: POST `/tasks/{taskId}/subtasks/{id}/delete` → redirect `/tasks/{taskId}`.
- UI: thêm `onclick="return confirm('Are you sure?')"` ở button xoá.

---

## 9) Thymeleaf: UX & Helper
- Badge hiển thị `priority` (LOW…URGENT) và `status` (TODO…)
- Format ngày `dueDate` (`#temporals.format(dueDate, 'yyyy-MM-dd')`).
- Flash message: include `fragments/_flash.html` trong layout.
- Nếu chưa login: navbar ẩn các mục Tasks/Profile; nếu login: hiển thị username và nút Logout (form POST).

---

## 10) Controller mẫu (phác thảo chữ, không code)
**TaskController**
- `list(ownerId, filter, pageable, model)` → `model.addAttribute("page", page)`; `return "task/list";`
- `newForm(model)` → `model.addAttribute("form", new TaskCreateDto(...))` → `return "task/form";`
- `create(@Valid form, BindingResult br, RedirectAttributes ra)` → on error `return "task/form";` else `ra.addFlashAttribute("success", "Created"); return "redirect:/tasks";`
- `detail(ownerId, id, model)` → load Task + subtasks → `return "task/detail";`
- `editForm(ownerId, id, model)` → map sang update dto → `return "task/form";`
- `update(ownerId, id, @Valid dto, BindingResult br, RedirectAttributes ra)` → on error `return "task/form";` else redirect detail.
- `changeStatus(ownerId, id, @RequestParam status)` → service.changeStatus → redirect detail.
- `delete(ownerId, id)` → service.delete → redirect list.

**SubtaskController**
- `create(ownerId, taskId, @Valid dto, ...)` → redirect detail.
- `update(ownerId, taskId, id, @Valid dto, ...)` → redirect detail.
- `toggleStatus(ownerId, taskId, id)` → redirect detail.
- `delete(ownerId, taskId, id)` → redirect detail.

---

## 11) Security & CSRF
- Tất cả form POST có thẻ CSRF (Thymeleaf tự render nếu dùng `<form th:action>` + dialect).
- Nút **Logout** phải là form POST tới `/logout` (P1 đã cấu hình).
- Không render action nếu user không sở hữu resource (UI hygiene).

---

## 12) Pagination (tuỳ chọn)
- Dùng `Page<Task>` ở list. Fragment `fragments/_pagination.html` nhận `page` và vẽ prev/next/query params.
- Nếu chưa làm pagination, set `size=100` tạm thời, sau thêm sau.

---

## 13) Phân rã Task nhỏ (cho AI Agent)
**T3.1 — TaskController + Views (List/Create/Edit/Delete/Status)**
- [ ] Controller: routes `/tasks`… theo spec.
- [ ] `task/list.html`: filter/search/sort + bảng.
- [ ] `task/form.html`: tạo & sửa (dùng chung); bind errors; hidden `version`.
- [ ] Flash message cho create/update/delete.

**T3.2 — Task Detail + Subtasks Panel**
- [ ] `task/detail.html` hiển thị task + include `subtask/_list.html` & `_form.html`.
- [ ] Nút đổi status nhanh ở task.

**T3.3 — SubtaskController + Fragments**
- [ ] Controller: create/update/toggle/delete theo routes.
- [ ] `subtask/_list.html`: danh sách + actions.
- [ ] `subtask/_form.html`: form thêm/sửa inline.

**T3.4 — Ownership & Security in UI**
- [ ] Không render action nếu không thuộc owner (giữ code gọn gàng; service vẫn kiểm tra).
- [ ] Tất cả form POST có CSRF.

**T3.5 — Optional: Pagination & Sorting Helpers**
- [ ] Fragment `_pagination.html`.
- [ ] Helper build URL giữ nguyên params khi đổi trang/sort.

**T3.6 — Validation UX**
- [ ] Hiển thị `th:errors` tại từng input.
- [ ] Giữ lại giá trị người dùng nhập khi lỗi.

---

## 14) Acceptance Criteria chi tiết (Checklist)
- [ ] `/tasks` hiển thị đúng **Task của current user**; filter/sort/search chạy.
- [ ] Tạo Task hợp lệ → flash “Created”, hiển thị trong list; invalid → lỗi hiển thị ở form.
- [ ] Sửa Task với `version` đúng → cập nhật thành công; `version` sai (test thủ công) → báo lỗi hợp lý (P4 handler).
- [ ] Đổi status Task nhanh (POST `/tasks/{id}/status`) theo policy đã chọn.
- [ ] `/tasks/{id}` hiển thị subtask; thêm/sửa/xoá/toggle hoạt động; redirect về detail với flash.
- [ ] Xoá Task → toàn bộ subtask bị xoá; confirm dialog xuất hiện.

---

## 15) Rủi ro & Lưu ý
- **Bỏ sót CSRF** ở form POST ⇒ 403.
- **Không truyền `version` khi update Task** ⇒ dễ lost update; luôn thêm `<input type="hidden" name="version" ...>`.
- **Filter/search/sort thiếu ownerId** ở service ⇒ lộ dữ liệu: luôn truyền `ownerId` vào service.
- **UX**: phân biệt rõ badge màu cho priority/status; định dạng ngày nhất quán.

---

## 16) Ước lượng
- T3.1–T3.3: ~0.75–1 ngày
- T3.4–T3.6: ~0.5 ngày

> Tổng P3: **~1.25–1.5 ngày** tập trung.


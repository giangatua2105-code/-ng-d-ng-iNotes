# [Case Study] Ứng dụng iNotes – Hướng dẫn làm từng bước

Ứng dụng quản lý ghi chú **iNotes** (Java Servlet + JSP + JDBC) áp dụng **Strategy Pattern**
để chuyển đổi linh hoạt giữa lưu trữ **MySQL** và lưu **File txt** ngay lúc chạy.

---

## 0. Chuẩn bị môi trường

| Công cụ | Phiên bản gợi ý |
|---|---|
| JDK | 11 trở lên |
| Maven | 3.6+ (IntelliJ đã có sẵn) |
| MySQL | 8.x |
| Tomcat | **9.x** (dùng `javax.servlet`). Không dùng Tomcat 10+ vì đã đổi sang `jakarta.*` |
| IDE | IntelliJ IDEA (Ultimate để chạy Tomcat) hoặc Eclipse/NetBeans |

---

## 1. Cấu trúc project

```
inotes/
├── pom.xml
├── HUONG_DAN.md
├── database/
│   └── inotes_db.sql                     <- Bước 1
└── src/main/
    ├── java/com/inotes/
    │   ├── config/
    │   │   ├── AppConfig.java            (đọc db.properties)
    │   │   └── DBConnection.java         (kết nối JDBC)
    │   ├── model/
    │   │   ├── Note.java                 <- Bước 2
    │   │   └── NoteType.java
    │   ├── strategy/                     <- Bước 6
    │   │   ├── NoteStrategy.java         (interface)
    │   │   ├── NoteDBStrategy.java       (MySQL)
    │   │   ├── NoteFileStrategy.java     (File id|title|content|typeId)
    │   │   └── StorageException.java
    │   ├── service/
    │   │   └── NoteManagement.java       <- Bước 3 (+ Context của Strategy)
    │   └── controller/
    │       └── NoteServlet.java          (điều hướng Bước 4, 5)
    ├── resources/
    │   └── db.properties                 (user/password MySQL, storage mặc định)
    └── webapp/
        ├── index.jsp
        ├── css/style.css
        └── WEB-INF/
            ├── web.xml
            └── views/
                ├── list.jsp              <- Bước 5
                ├── form.jsp              <- Bước 4
                └── view.jsp
```

**Luồng xử lý:** Trình duyệt → `NoteServlet` → `NoteManagement` → `NoteStrategy` (DB hoặc File) → trả dữ liệu → JSP hiển thị.

---

## Bước 1. Thiết kế cơ sở dữ liệu `inotes_db`

File: `database/inotes_db.sql`

1. Mở MySQL Workbench (hoặc terminal) và chạy script:
   ```bash
   mysql -u root -p < database/inotes_db.sql
   ```
2. Script tạo database `inotes_db` (utf8mb4 để lưu được tiếng Việt) và 2 bảng quan hệ **1 – N**:

   | Bảng | Cột | Kiểu | Ràng buộc |
   |---|---|---|---|
   | `note_type` | id | INT | PK, AUTO_INCREMENT |
   | | name | VARCHAR(100) | NOT NULL |
   | | description | VARCHAR(255) | |
   | `notes` | id | INT | PK, AUTO_INCREMENT |
   | | title | VARCHAR(255) | NOT NULL |
   | | content | TEXT | |
   | | type_id | INT | FK → `note_type(id)` |

3. Script chèn sẵn 3 thể loại (Cá nhân, Công việc, Học tập) và 3 ghi chú mẫu.
4. Kiểm tra: `SELECT * FROM notes;`

> Chỉ chạy script **một lần**. Chạy lại sẽ chèn trùng dữ liệu mẫu.

---

## Bước 2. Xây dựng Model `Note`

File: `model/Note.java`, `model/NoteType.java`

- Thuộc tính của `Note`: `id`, `title`, `content`, `typeId` (+ `typeName` để hiển thị tên thể loại).
- Có đủ các phương thức theo đề: `setTitle/getTitle`, `setContent/getContent`, `setTypeId/getTypeId`.
- `save()`: nếu `id == 0` là **thêm mới**, nếu `id > 0` là **cập nhật**.
- `delete(int id)`: xóa theo ID.
- **Điểm mấu chốt:** `Note` không tự viết SQL hay đọc/ghi file. Nó chỉ gọi
  `NoteManagement.getInstance().getStrategy().save(this)` – nghĩa là uỷ quyền cho strategy đang chọn.

---

## Bước 3. Lớp điều khiển `NoteManagement`

File: `service/NoteManagement.java`

| Phương thức | Việc làm |
|---|---|
| `searchNotes(String keyword)` | Tìm theo từ khóa trong **tiêu đề hoặc nội dung** |
| `addNote(title, content, typeId)` | Kiểm tra tiêu đề không rỗng, tạo `Note` và gọi `save()` |
| `deleteNote(int id)` | Xóa ghi chú |
| `updateNote(id, title, content, typeId)` | Phục vụ nút **Sửa** |
| `getAllNotes()`, `getNoteById(id)`, `getAllTypes()` | Phục vụ giao diện |

Lớp này là **singleton** (`getInstance()`), để cả ứng dụng dùng chung một lựa chọn lưu trữ.

---

## Bước 4. Giao diện Form thêm mới ghi chú

File: `webapp/WEB-INF/views/form.jsp` (Servlet: `GET /notes?action=create`)

- **Tiêu đề**: `<input type="text">` (bắt buộc nhập).
- **Phân loại**: `<select>` được nạp từ `getAllTypes()` bằng `<c:forEach>`.
- **Nội dung**: `<textarea>`.
- Nút **Lưu Ghi Chú** gửi `POST action=save`, nút **Hủy bỏ** quay về danh sách.
- Cùng form này dùng để **Sửa** (`?action=edit&id=...`): form tự điền sẵn dữ liệu cũ.
- Dùng `<c:out>` khi in dữ liệu ra để tránh lỗi XSS.

---

## Bước 5. Giao diện Danh sách và Tìm kiếm

File: `webapp/WEB-INF/views/list.jsp` (Servlet: `GET /notes`)

- Ô **Tìm kiếm** gửi `GET /notes?keyword=...` → `searchNotes(keyword)`.
- Nút **+ Thêm mới** mở form Bước 4.
- Bảng: `#` | `Tiêu đề` | `Phân loại` | `Thao tác (Xem | Sửa | Xóa)`.
- **Xóa** dùng form `POST` có hộp thoại xác nhận (không dùng link GET để xóa, tránh bị xóa nhầm).
- Sau khi Thêm/Sửa/Xóa, Servlet dùng `redirect` (mẫu **Post-Redirect-Get**) và hiện thông báo một lần.

---

## Bước 6. Áp dụng Strategy Pattern

```
                    ┌──────────────────────┐
  NoteServlet ───►  │   NoteManagement     │   (Context)
                    │ - strategy           │
                    │ + setStrategy(...)   │
                    └──────────┬───────────┘
                               │ dùng
                    ┌──────────▼───────────┐
                    │ «interface»          │
                    │ NoteStrategy         │
                    │ save / delete        │
                    │ findAll / search     │
                    └──────────┬───────────┘
                 ┌─────────────┴─────────────┐
       ┌─────────▼─────────┐       ┌─────────▼──────────┐
       │  NoteDBStrategy   │       │ NoteFileStrategy   │
       │  MySQL + JDBC     │       │ id|title|content|  │
       └───────────────────┘       │ typeId  (.txt)     │
                                   └────────────────────┘
```

**6.1 Interface `NoteStrategy`** – hợp đồng chuẩn: `save(Note)`, `delete(int)`, `findAll()`, `search(String)`.
Mình bổ sung thêm `findById(int)` và `findAllTypes()` để phục vụ màn hình Xem/Sửa và ô chọn Phân loại.

**6.2 `NoteDBStrategy`** – dùng `PreparedStatement` (chống SQL Injection):
- thêm: `INSERT ... RETURN_GENERATED_KEYS` để lấy id mới;
- sửa: `UPDATE ... WHERE id = ?`;
- tìm kiếm: `WHERE title LIKE ? OR content LIKE ?`;
- danh sách: `JOIN note_type` để lấy tên thể loại.

**6.3 `NoteFileStrategy`** – mỗi ghi chú một dòng `id|title|content|typeId`:
- id tự tăng = (id lớn nhất) + 1;
- nội dung có ký tự `|`, `\` hoặc xuống dòng được **escape** (`\|`, `\\`, `\n`) nên không làm hỏng file;
- mặc định lưu ở `~/inotes-data/notes.txt`, đổi bằng `file.path` trong `db.properties`;
- thể loại là danh sách cố định (Cá nhân/Công việc/Học tập), trùng id với dữ liệu MySQL.

**6.4 Đổi strategy lúc chạy** – `NoteManagement` có `private NoteStrategy strategy;` và
`setStrategy(NoteStrategy)`. Ở trang danh sách có 2 nút **MySQL / File (txt)**; bấm nút là
Servlet gọi `manager.setStrategy(new NoteFileStrategy())` (hoặc `NoteDBStrategy`) – **JSP và
Servlet không cần sửa gì** vì chỉ làm việc qua interface. Đó chính là *Runtime Pluggable Storage*.

---

## 7. Cấu hình và chạy ứng dụng

### 7.1 Cấu hình MySQL
Sửa `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/inotes_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
db.user=root
db.password=MAT_KHAU_CUA_BAN
storage.default=db      # hoặc file nếu muốn chạy thử chưa cần MySQL
```

### 7.2 Cách A – IntelliJ IDEA + Tomcat 9
1. `File → Open` → chọn thư mục `inotes` (IntelliJ nhận ra `pom.xml`, tự tải thư viện).
2. `Run → Edit Configurations → + → Tomcat Server → Local`, chọn thư mục Tomcat 9.
3. Tab **Deployment** → `+ → Artifact… → inotes:war exploded`; **Application context** = `/inotes`.
4. Bấm **Run**, mở: <http://localhost:8080/inotes/>

### 7.3 Cách B – Dòng lệnh, không cần Tomcat (Jetty plugin)
```bash
mvn jetty:run
```
Mở: <http://localhost:8080/inotes/>

### 7.4 Cách C – Đóng gói WAR rồi copy vào Tomcat
```bash
mvn clean package
# copy target/inotes.war vào <TOMCAT>/webapps/ rồi chạy bin/startup.sh (hoặc startup.bat)
```

---

## 8. Danh sách kiểm thử (tự test trước khi nộp)

| # | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Mở trang chủ (strategy MySQL) | Thấy 3 ghi chú mẫu, cột Phân loại hiển thị đúng tên |
| 2 | `+ Thêm mới` → nhập đủ → **Lưu Ghi Chú** | Quay về danh sách, thấy ghi chú mới và thông báo |
| 3 | Thêm với tiêu đề rỗng | Báo lỗi "Tiêu đề không được để trống" |
| 4 | Tìm "pattern" | Chỉ còn ghi chú chứa "pattern" ở tiêu đề hoặc nội dung |
| 5 | **Xem / Sửa** một ghi chú | Xem được chi tiết; sửa xong danh sách cập nhật |
| 6 | **Xóa** | Có hộp xác nhận, bản ghi biến mất |
| 7 | Bấm nút **File (txt)** rồi thêm vài ghi chú | Thêm được; file `~/inotes-data/notes.txt` có các dòng `id\|title\|content\|typeId` |
| 8 | Bấm lại **MySQL** | Dữ liệu MySQL còn nguyên, không lẫn với dữ liệu file |
| 9 | Tắt MySQL rồi mở trang | Hiện thông báo lỗi nhưng vẫn bấm được nút **File (txt)** để dùng tiếp |

---

## 9. Lỗi thường gặp

| Lỗi | Cách xử lý |
|---|---|
| `Communications link failure` / `Access denied` | MySQL chưa chạy hoặc sai `db.user`/`db.password` trong `db.properties` |
| `Unknown database 'inotes_db'` | Chưa chạy `inotes_db.sql` |
| `ClassNotFoundException: javax.servlet...` hoặc 404 với mọi URL | Đang dùng Tomcat 10+; hãy dùng **Tomcat 9** |
| Tiếng Việt bị `???` | Giữ nguyên `useUnicode=true&characterEncoding=UTF-8` trong URL; không đổi encoding của file `.jsp` khỏi UTF-8 |
| Ô Phân loại trống | Bảng `note_type` chưa có dữ liệu (chạy lại phần `INSERT` trong script) |

---

## 10. Nộp bài

Ô **Link** yêu cầu một đường dẫn URL, thường là repo GitHub:

```bash
cd inotes
git init
git add .
git commit -m "iNotes - Strategy Pattern"
git branch -M main
git remote add origin https://github.com/<tai-khoan>/inotes.git
git push -u origin main
```

Dán đường dẫn `https://github.com/<tai-khoan>/inotes` vào ô **Link** rồi bấm **Nộp bài**.
Nếu giảng viên yêu cầu nộp file, hãy nộp luôn file `inotes.zip` này.

-- ============================================================
-- iNotes - Buoc 1: Thiet ke co so du lieu inotes_db
-- Chay 1 lan:  mysql -u root -p < database/inotes_db.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS inotes_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE inotes_db;

-- Bang the loai (phia "1" cua quan he 1 - N)
CREATE TABLE IF NOT EXISTS note_type (
    id          INT           NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- Bang ghi chu (phia "N"), type_id tham chieu note_type(id)
CREATE TABLE IF NOT EXISTS notes (
    id      INT          NOT NULL AUTO_INCREMENT,
    title   VARCHAR(255) NOT NULL,
    content TEXT,
    type_id INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_notes_note_type
        FOREIGN KEY (type_id) REFERENCES note_type (id)
) ENGINE = InnoDB;

-- Du lieu mau (id 1,2,3 trung voi danh sach the loai mac dinh cua NoteFileStrategy)
INSERT INTO note_type (name, description) VALUES
    ('Cá nhân',   'Ghi chú việc riêng, sở thích, gia đình'),
    ('Công việc', 'Ghi chú họp hành, công việc cần làm'),
    ('Học tập',   'Ghi chú bài giảng, ôn tập, tài liệu');

INSERT INTO notes (title, content, type_id) VALUES
    ('Họp giao ban đầu tuần',        'Chuẩn bị báo cáo tiến độ dự án trước 9h sáng thứ Hai.', 2),
    ('Ôn tập Design Pattern Servlet', 'Xem lại Strategy, Singleton, MVC và cách áp dụng trong Servlet.', 3),
    ('Mua quà sinh nhật mẹ',          'Đặt hoa và bánh trước ngày 20.', 1);

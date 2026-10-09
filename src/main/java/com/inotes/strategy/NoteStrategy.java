package com.inotes.strategy;

import com.inotes.model.Note;
import com.inotes.model.NoteType;

import java.util.List;

/**
 * Bước 6: Interface Strategy - "hợp đồng" chung cho mọi tầng lưu trữ.
 * NoteManagement chỉ làm việc với interface này, không biết bên dưới là MySQL hay File.
 */
public interface NoteStrategy {

    /** Thêm mới (note.id == 0, strategy tự gán id) hoặc cập nhật (note.id > 0). */
    void save(Note note);

    void delete(int id);

    List<Note> findAll();

    /** Tìm theo từ khóa trong tiêu đề hoặc nội dung (không phân biệt hoa/thường). */
    List<Note> search(String keyword);

    /** Phục vụ màn hình Xem / Sửa. Trả về null nếu không có. */
    Note findById(int id);

    /** Phục vụ ô chọn "Phân loại". */
    List<NoteType> findAllTypes();
}

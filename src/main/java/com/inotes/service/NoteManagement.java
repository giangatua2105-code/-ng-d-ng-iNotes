package com.inotes.service;

import com.inotes.config.AppConfig;
import com.inotes.model.Note;
import com.inotes.model.NoteType;
import com.inotes.strategy.NoteDBStrategy;
import com.inotes.strategy.NoteFileStrategy;
import com.inotes.strategy.NoteStrategy;

import java.util.List;

/**
 * Bước 3 + 6: Lớp điều khiển nghiệp vụ (Context của Strategy Pattern).
 * Mọi thao tác dữ liệu đều đi qua thuộc tính {@code strategy}; có thể đổi
 * strategy ngay lúc chạy bằng {@link #setStrategy(NoteStrategy)}.
 * Là singleton để toàn bộ ứng dụng dùng chung một lựa chọn lưu trữ.
 */
public class NoteManagement {

    private static final NoteManagement INSTANCE = new NoteManagement();

    private volatile NoteStrategy strategy;

    private NoteManagement() {
        boolean useFile = "file".equalsIgnoreCase(AppConfig.get("storage.default", "db"));
        this.strategy = useFile ? new NoteFileStrategy() : new NoteDBStrategy();
    }

    public static NoteManagement getInstance() {
        return INSTANCE;
    }

    // ---------------------------------------------------- Strategy (Runtime)

    public NoteStrategy getStrategy() {
        return strategy;
    }

    /** Runtime Pluggable Storage: đổi tầng lưu trữ mà không đụng tới Servlet/JSP. */
    public void setStrategy(NoteStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("strategy must not be null");
        }
        this.strategy = strategy;
    }

    // ------------------------------------------------------ Nghiệp vụ Bước 3

    /** Tìm ghi chú theo từ khóa trong tiêu đề hoặc nội dung. */
    public List<Note> searchNotes(String keyword) {
        return strategy.search(keyword);
    }

    /** Tạo mới và lưu ghi chú. */
    public Note addNote(String title, String content, int typeId) {
        requireTitle(title);
        Note note = new Note(0, title.trim(), content, typeId);
        note.save();
        return note;
    }

    /** Cập nhật ghi chú đã có (chức năng "Sửa"). */
    public Note updateNote(int id, String title, String content, int typeId) {
        requireTitle(title);
        Note note = new Note(id, title.trim(), content, typeId);
        note.save();
        return note;
    }

    /** Xóa ghi chú theo ID. */
    public void deleteNote(int id) {
        new Note().delete(id);
    }

    // ------------------------------------------------- Phục vụ giao diện

    public List<Note> getAllNotes() {
        return strategy.findAll();
    }

    public Note getNoteById(int id) {
        return strategy.findById(id);
    }

    public List<NoteType> getAllTypes() {
        return strategy.findAllTypes();
    }

    private void requireTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề không được để trống");
        }
    }
}

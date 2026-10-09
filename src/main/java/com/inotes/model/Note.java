package com.inotes.model;

import com.inotes.service.NoteManagement;

/**
 * Bước 2: Model ghi chú.
 * save() và delete(int) không tự biết lưu ở đâu: chúng uỷ quyền cho
 * strategy đang được chọn trong NoteManagement (DB hoặc File).
 */
public class Note {

    private int id;            // 0 = ghi chú mới chưa lưu
    private String title;
    private String content;
    private int typeId;
    private String typeName;   // chỉ để hiển thị (JOIN note_type)

    public Note() {
    }

    public Note(int id, String title, String content, int typeId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.typeId = typeId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    /** Lưu mới (id = 0) hoặc cập nhật (id > 0) vào hệ thống lưu trữ hiện tại. */
    public void save() {
        NoteManagement.getInstance().getStrategy().save(this);
    }

    /** Xóa ghi chú theo ID khỏi hệ thống lưu trữ hiện tại. */
    public void delete(int id) {
        NoteManagement.getInstance().getStrategy().delete(id);
    }
}

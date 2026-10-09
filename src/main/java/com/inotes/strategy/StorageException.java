package com.inotes.strategy;

/** Lỗi khi đọc/ghi tầng lưu trữ (SQL hoặc I/O) - bọc lại để Servlet hiển thị thông báo. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}

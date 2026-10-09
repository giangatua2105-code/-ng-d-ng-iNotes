package com.inotes.strategy;

import com.inotes.config.AppConfig;
import com.inotes.model.Note;
import com.inotes.model.NoteType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Chiến lược lưu trữ ra tập tin văn bản, mỗi dòng một ghi chú:
 *
 *     id|title|content|typeId
 *
 * Để title/content chứa được ký tự '|' và xuống dòng, dữ liệu được escape:
 *     '\' -> '\\'    '|' -> '\|'    xuống dòng -> '\n'
 */
public class NoteFileStrategy implements NoteStrategy {

    /** Thể loại mặc định (khớp id với dữ liệu mẫu trong inotes_db.sql). */
    private static final List<NoteType> TYPES = Arrays.asList(
            new NoteType(1, "Cá nhân", "Ghi chú việc riêng, sở thích, gia đình"),
            new NoteType(2, "Công việc", "Ghi chú họp hành, công việc cần làm"),
            new NoteType(3, "Học tập", "Ghi chú bài giảng, ôn tập, tài liệu"));

    private final Path file;

    public NoteFileStrategy() {
        this(Paths.get(AppConfig.get("file.path",
                System.getProperty("user.home") + "/inotes-data/notes.txt")));
    }

    public NoteFileStrategy(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    @Override
    public synchronized void save(Note note) {
        List<Note> notes = readAll();
        if (note.getId() == 0) {
            int nextId = 1;
            for (Note n : notes) {
                nextId = Math.max(nextId, n.getId() + 1);
            }
            note.setId(nextId);
            notes.add(note);
        } else {
            boolean found = false;
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getId() == note.getId()) {
                    notes.set(i, note);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new StorageException("Không tìm thấy ghi chú id = " + note.getId());
            }
        }
        writeAll(notes);
    }

    @Override
    public synchronized void delete(int id) {
        List<Note> notes = readAll();
        if (notes.removeIf(n -> n.getId() == id)) {
            writeAll(notes);
        }
    }

    @Override
    public synchronized List<Note> findAll() {
        return readAll();
    }

    @Override
    public synchronized List<Note> search(String keyword) {
        List<Note> all = readAll();
        if (keyword == null || keyword.trim().isEmpty()) {
            return all;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        List<Note> result = new ArrayList<>();
        for (Note n : all) {
            if (contains(n.getTitle(), kw) || contains(n.getContent(), kw)) {
                result.add(n);
            }
        }
        return result;
    }

    @Override
    public synchronized Note findById(int id) {
        for (Note n : readAll()) {
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    @Override
    public List<NoteType> findAllTypes() {
        return new ArrayList<>(TYPES);
    }

    // ------------------------------------------------------------------ I/O

    private List<Note> readAll() {
        List<Note> notes = new ArrayList<>();
        if (!Files.exists(file)) {
            return notes;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Note note = parseLine(line);
                if (note != null) {
                    notes.add(note);
                }
            }
        } catch (IOException e) {
            throw new StorageException("Không thể đọc tập tin " + file + ": " + e.getMessage(), e);
        }
        return notes;
    }

    private void writeAll(List<Note> notes) {
        List<String> lines = new ArrayList<>();
        for (Note n : notes) {
            lines.add(n.getId() + "|" + escape(n.getTitle()) + "|" + escape(n.getContent()) + "|" + n.getTypeId());
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Không thể ghi tập tin " + file + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------- encode / decode

    private Note parseLine(String line) {
        List<String> f = splitFields(line);
        if (f.size() != 4) {
            return null; // dòng hỏng -> bỏ qua
        }
        try {
            int id = Integer.parseInt(f.get(0).trim());
            int typeId = Integer.parseInt(f.get(3).trim());
            Note note = new Note(id, f.get(1), f.get(2), typeId);
            note.setTypeName(typeNameOf(typeId));
            return note;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Tách theo '|' chưa bị escape, đồng thời giải mã \\, \| và \n. */
    private List<String> splitFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 1 < line.length()) {
                char next = line.charAt(++i);
                cur.append(next == 'n' ? '\n' : next);
            } else if (ch == '|') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        fields.add(cur.toString());
        return fields;
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '|':  sb.append("\\|");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': break;
                default:   sb.append(ch);
            }
        }
        return sb.toString();
    }

    private String typeNameOf(int typeId) {
        for (NoteType t : TYPES) {
            if (t.getId() == typeId) {
                return t.getName();
            }
        }
        return null;
    }

    private boolean contains(String text, String lowerKeyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }
}

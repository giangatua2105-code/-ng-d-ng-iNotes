package com.inotes.strategy;

import com.inotes.config.DBConnection;
import com.inotes.model.Note;
import com.inotes.model.NoteType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Chiến lược lưu trữ bằng MySQL qua JDBC (database inotes_db). */
public class NoteDBStrategy implements NoteStrategy {

    private static final String SELECT_NOTE =
            "SELECT n.id, n.title, n.content, n.type_id, t.name AS type_name "
                    + "FROM notes n LEFT JOIN note_type t ON n.type_id = t.id ";

    @Override
    public void save(Note note) {
        try (Connection conn = DBConnection.getConnection()) {
            if (note.getId() == 0) {
                String sql = "INSERT INTO notes (title, content, type_id) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, note);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            note.setId(keys.getInt(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE notes SET title = ?, content = ?, type_id = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    bind(ps, note);
                    ps.setInt(4, note.getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Không thể lưu ghi chú vào MySQL: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, Note note) throws SQLException {
        ps.setString(1, note.getTitle());
        ps.setString(2, note.getContent());
        if (note.getTypeId() > 0) {
            ps.setInt(3, note.getTypeId());
        } else {
            ps.setNull(3, Types.INTEGER);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Không thể xóa ghi chú trong MySQL: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Note> findAll() {
        return query(SELECT_NOTE + "ORDER BY n.id", null);
    }

    @Override
    public List<Note> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String like = "%" + keyword.trim() + "%";
        return query(SELECT_NOTE + "WHERE n.title LIKE ? OR n.content LIKE ? ORDER BY n.id", like);
    }

    @Override
    public Note findById(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_NOTE + "WHERE n.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapNote(rs) : null;
            }
        } catch (SQLException e) {
            throw new StorageException("Không thể đọc ghi chú từ MySQL: " + e.getMessage(), e);
        }
    }

    @Override
    public List<NoteType> findAllTypes() {
        List<NoteType> types = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, description FROM note_type ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                types.add(new NoteType(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
            }
        } catch (SQLException e) {
            throw new StorageException("Không thể đọc thể loại từ MySQL: " + e.getMessage(), e);
        }
        return types;
    }

    /** Chạy câu SELECT; nếu likeParam != null thì gán cho cả 2 dấu ? (title, content). */
    private List<Note> query(String sql, String likeParam) {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (likeParam != null) {
                ps.setString(1, likeParam);
                ps.setString(2, likeParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Không thể đọc danh sách ghi chú từ MySQL: " + e.getMessage(), e);
        }
        return notes;
    }

    private Note mapNote(ResultSet rs) throws SQLException {
        Note note = new Note(rs.getInt("id"), rs.getString("title"),
                rs.getString("content"), rs.getInt("type_id"));
        note.setTypeName(rs.getString("type_name"));
        return note;
    }
}

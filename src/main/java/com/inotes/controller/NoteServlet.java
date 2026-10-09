package com.inotes.controller;

import com.inotes.model.Note;
import com.inotes.service.NoteManagement;
import com.inotes.strategy.NoteDBStrategy;
import com.inotes.strategy.NoteFileStrategy;
import com.inotes.strategy.StorageException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;

/**
 * Bước 4 + 5: Controller duy nhất, điều hướng theo tham số ?action=
 *
 *   GET  /notes                  -> danh sách + tìm kiếm  (?keyword=...)
 *   GET  /notes?action=create    -> form thêm mới
 *   GET  /notes?action=edit&id=  -> form sửa
 *   GET  /notes?action=view&id=  -> xem chi tiết
 *   POST action=save             -> thêm mới / cập nhật
 *   POST action=delete           -> xóa
 *   POST action=strategy&type=db|file -> đổi tầng lưu trữ lúc chạy
 */
@WebServlet(name = "NoteServlet", urlPatterns = "/notes")
public class NoteServlet extends HttpServlet {

    private static final String LIST = "/WEB-INF/views/list.jsp";
    private static final String FORM = "/WEB-INF/views/form.jsp";
    private static final String VIEW = "/WEB-INF/views/view.jsp";

    private final NoteManagement manager = NoteManagement.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }
        try {
            switch (action) {
                case "create":
                    showForm(req, resp, new Note());
                    break;
                case "edit":
                    showEditOrView(req, resp, FORM, true);
                    break;
                case "view":
                    showEditOrView(req, resp, VIEW, false);
                    break;
                default:
                    showList(req, resp);
            }
        } catch (StorageException e) {
            showListWithError(req, resp, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        if (action == null) {
            action = "";
        }
        try {
            switch (action) {
                case "save":
                    saveNote(req, resp);
                    break;
                case "delete":
                    manager.deleteNote(parseInt(req.getParameter("id")));
                    flash(req, "Đã xóa ghi chú.");
                    resp.sendRedirect(req.getContextPath() + "/notes");
                    break;
                case "strategy":
                    changeStrategy(req, resp);
                    break;
                default:
                    resp.sendRedirect(req.getContextPath() + "/notes");
            }
        } catch (StorageException e) {
            showListWithError(req, resp, e.getMessage());
        }
    }

    // ------------------------------------------------------------- handlers

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = req.getParameter("keyword");
        req.setAttribute("notes", manager.searchNotes(keyword));
        req.setAttribute("keyword", keyword);
        prepareListPage(req);
        req.getRequestDispatcher(LIST).forward(req, resp);
    }

    /** Khi tầng lưu trữ lỗi (vd: chưa bật MySQL), vẫn hiển thị trang để người dùng đổi sang File. */
    private void showListWithError(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        req.setAttribute("notes", Collections.emptyList());
        req.setAttribute("error", message);
        prepareListPage(req);
        req.getRequestDispatcher(LIST).forward(req, resp);
    }

    private void prepareListPage(HttpServletRequest req) {
        req.setAttribute("strategyKey",
                manager.getStrategy() instanceof NoteFileStrategy ? "file" : "db");
        HttpSession session = req.getSession();
        Object msg = session.getAttribute("flash");
        if (msg != null) {
            req.setAttribute("flash", msg);
            session.removeAttribute("flash");
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Note note)
            throws ServletException, IOException {
        req.setAttribute("note", note);
        req.setAttribute("types", manager.getAllTypes());
        req.getRequestDispatcher(FORM).forward(req, resp);
    }

    private void showEditOrView(HttpServletRequest req, HttpServletResponse resp, String page, boolean needTypes)
            throws ServletException, IOException {
        Note note = manager.getNoteById(parseInt(req.getParameter("id")));
        if (note == null) {
            flash(req, "Không tìm thấy ghi chú.");
            resp.sendRedirect(req.getContextPath() + "/notes");
            return;
        }
        req.setAttribute("note", note);
        if (needTypes) {
            req.setAttribute("types", manager.getAllTypes());
        }
        req.getRequestDispatcher(page).forward(req, resp);
    }

    private void saveNote(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id = parseInt(req.getParameter("id"));
        String title = req.getParameter("title");
        String content = req.getParameter("content");
        int typeId = parseInt(req.getParameter("typeId"));

        if (title == null || title.trim().isEmpty()) {
            req.setAttribute("error", "Tiêu đề không được để trống.");
            showForm(req, resp, new Note(id, title, content, typeId));
            return;
        }
        if (id > 0) {
            manager.updateNote(id, title, content, typeId);
            flash(req, "Đã cập nhật ghi chú.");
        } else {
            manager.addNote(title, content, typeId);
            flash(req, "Đã thêm ghi chú mới.");
        }
        resp.sendRedirect(req.getContextPath() + "/notes");
    }

    private void changeStrategy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String type = req.getParameter("type");
        if ("file".equals(type)) {
            manager.setStrategy(new NoteFileStrategy());
            flash(req, "Đã chuyển sang lưu trữ bằng File.");
        } else {
            manager.setStrategy(new NoteDBStrategy());
            flash(req, "Đã chuyển sang lưu trữ bằng MySQL.");
        }
        resp.sendRedirect(req.getContextPath() + "/notes");
    }

    // -------------------------------------------------------------- helpers

    private void flash(HttpServletRequest req, String message) {
        req.getSession().setAttribute("flash", message);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

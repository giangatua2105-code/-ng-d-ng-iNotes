<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="editing" value="${not empty note and note.id gt 0}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>iNotes - ${editing ? 'Sửa ghi chú' : 'Thêm mới ghi chú'}</title>
    <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<div class="container">
    <h1>${editing ? '✏️ Sửa Ghi Chú' : '➕ Thêm Mới Ghi Chú'}</h1>
    <p class="subtitle">iNotes</p>

    <c:if test="${not empty error}">
        <div class="alert error"><c:out value="${error}"/></div>
    </c:if>

    <form class="card" method="post" action="${ctx}/notes">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="id" value="${editing ? note.id : 0}">

        <div class="field">
            <label for="title">Tiêu đề</label>
            <input type="text" id="title" name="title" maxlength="255" required
                   value="<c:out value='${note.title}'/>" placeholder="Nhập tiêu đề ghi chú">
        </div>

        <div class="field">
            <label for="typeId">Phân loại</label>
            <select id="typeId" name="typeId">
                <c:forEach items="${types}" var="t">
                    <option value="${t.id}" ${t.id == note.typeId ? 'selected' : ''}>
                        <c:out value="${t.name}"/>
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="field">
            <label for="content">Nội dung</label>
            <textarea id="content" name="content" placeholder="Nhập nội dung chi tiết..."><c:out value="${note.content}"/></textarea>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn success">Lưu Ghi Chú</button>
            <a class="btn secondary" href="${ctx}/notes">Hủy bỏ</a>
        </div>
    </form>
</div>
</body>
</html>

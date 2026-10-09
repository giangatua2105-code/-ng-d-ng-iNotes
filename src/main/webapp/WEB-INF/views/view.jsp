<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>iNotes - <c:out value="${note.title}"/></title>
    <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<div class="container">
    <h1><c:out value="${note.title}"/></h1>
    <p class="meta">
        <c:if test="${not empty note.typeName}">
            <span class="badge"><c:out value="${note.typeName}"/></span>
        </c:if>
    </p>

    <div class="card">
        <div class="note-content"><c:out value="${note.content}"/></div>
    </div>

    <div class="form-actions">
        <a class="btn" href="${ctx}/notes?action=edit&id=${note.id}">Sửa</a>
        <a class="btn secondary" href="${ctx}/notes">Quay lại danh sách</a>
    </div>
</div>
</body>
</html>

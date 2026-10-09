<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>iNotes - Danh sách ghi chú</title>
    <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<div class="container">
    <h1>📝 iNotes Dashboard</h1>
    <p class="subtitle">Quản lý ghi chú cá nhân</p>

    <c:if test="${not empty flash}">
        <div class="alert ok"><c:out value="${flash}"/></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert error"><c:out value="${error}"/></div>
    </c:if>

    <%-- Chuyển đổi tầng lưu trữ lúc chạy (Strategy Pattern) --%>
    <div class="card storage">
        <span class="label">Tầng lưu trữ:</span>
        <form method="post" action="${ctx}/notes">
            <input type="hidden" name="action" value="strategy">
            <input type="hidden" name="type" value="db">
            <button type="submit" class="pill ${strategyKey == 'db' ? 'active' : ''}">🗄️ MySQL</button>
        </form>
        <form method="post" action="${ctx}/notes">
            <input type="hidden" name="action" value="strategy">
            <input type="hidden" name="type" value="file">
            <button type="submit" class="pill ${strategyKey == 'file' ? 'active' : ''}">📄 File (txt)</button>
        </form>
    </div>

    <div class="card">
        <div class="toolbar">
            <form method="get" action="${ctx}/notes">
                <input type="text" name="keyword" value="<c:out value='${keyword}'/>"
                       placeholder="Tìm theo tiêu đề hoặc nội dung...">
                <button type="submit" class="btn">Tìm kiếm</button>
            </form>
            <a class="btn success" href="${ctx}/notes?action=create">+ Thêm mới</a>
        </div>
    </div>

    <div class="card">
        <table>
            <thead>
            <tr>
                <th>#</th>
                <th>Tiêu đề</th>
                <th>Phân loại</th>
                <th>Thao tác</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${notes}" var="n" varStatus="st">
                <tr>
                    <td class="num">${st.count}</td>
                    <td><c:out value="${n.title}"/></td>
                    <td>
                        <c:if test="${not empty n.typeName}">
                            <span class="badge"><c:out value="${n.typeName}"/></span>
                        </c:if>
                    </td>
                    <td class="actions">
                        <a class="link" href="${ctx}/notes?action=view&id=${n.id}">Xem</a> |
                        <a class="link" href="${ctx}/notes?action=edit&id=${n.id}">Sửa</a> |
                        <form method="post" action="${ctx}/notes"
                              onsubmit="return confirm('Bạn chắc chắn muốn xóa ghi chú này?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${n.id}">
                            <button type="submit" class="link danger">Xóa</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty notes}">
                <tr><td colspan="4" class="empty">Chưa có ghi chú nào.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>

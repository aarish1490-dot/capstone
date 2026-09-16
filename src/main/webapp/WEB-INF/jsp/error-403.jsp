<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Access Denied"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<div class="empty-state">
    <h3>403 - Access denied</h3>
    <p><c:out value="${empty error ? 'You do not have permission to view this page.' : error}"/></p>
    <a href="${ctx}/" class="btn" style="margin-top:14px;">Go to Home</a>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>

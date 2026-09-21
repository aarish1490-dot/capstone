<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Order Placed"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<div class="form-card" style="max-width:560px;text-align:center;">
    <h2 style="color:var(--primary);">Order placed successfully!</h2>
    <p style="margin:12px 0;color:var(--gray);">
        Thank you for shopping at AarishMart. Your order has been recorded with mock payment confirmation.
    </p>
    <p style="font-size:1.1rem;">
        Order ID: <strong>#<c:out value="${order.id}"/></strong>
        &middot; Status: <span class="badge <c:choose>
            <c:when test="${order.status == 'PENDING'}">badge-pending</c:when>
            <c:when test="${order.status == 'CONFIRMED'}">badge-confirmed</c:when>
            <c:when test="${order.status == 'SHIPPED'}">badge-shipped</c:when>
            <c:when test="${order.status == 'DELIVERED'}">badge-delivered</c:when>
            <c:otherwise>badge-pending</c:otherwise>
        </c:choose>">
            <c:out value="${order.status}"/>
        </span>
    </p>
    <p style="font-size:1.3rem;font-weight:800;color:var(--primary);margin:14px 0;">
        Total: ₹ <fmt:formatNumber value="${order.totalAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
    </p>
    <a href="${ctx}/orders" class="btn" style="margin-right:8px;">View Order History</a>
    <a href="${ctx}/products" class="btn btn-secondary">Continue Shopping</a>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
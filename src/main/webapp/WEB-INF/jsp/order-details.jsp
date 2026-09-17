<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Order #${order.id}"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<nav class="breadcrumb">
    <a href="${ctx}/orders">Orders</a> / Order #<c:out value="${order.id}"/>
</nav>

<h2 class="page-title">Order Details</h2>
<p class="page-subtitle">
    Order <strong>#<c:out value="${order.id}"/></strong>
    &middot; <fmt:formatDate value="${order.createdAt}" pattern="dd MMM yyyy, HH:mm"/>
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

<div class="table-wrap">
    <table>
        <thead>
        <tr>
            <th>Product</th>
            <th>Quantity</th>
            <th>Unit Price</th>
            <th>Subtotal</th>
            <th>Review</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="item" items="${items}">
            <tr>
                <td class="name-cell">
                    <c:if test="${not empty item.imageUrl}">
                        <img class="order-thumb" src="<c:out value='${item.imageUrl}'/>" alt=""
                             onerror="this.onerror=null;this.src='${ctx}/images/placeholder.png'">
                    </c:if>
                    <a href="${ctx}/product?id=${item.productId}"><c:out value="${item.productName}"/></a>
                </td>
                <td><c:out value="${item.quantity}"/></td>
                <td>₹ <fmt:formatNumber value="${item.unitPrice}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                <td>₹ <fmt:formatNumber value="${item.unitPrice * item.quantity}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                <td>
                    <c:choose>
                        <c:when test="${reviewedProductIds.contains(item.productId)}">
                            <span class="reviewed-label">Reviewed</span>
                        </c:when>
                        <c:when test="${order.status == 'DELIVERED'}">
                            <a class="btn btn-sm btn-secondary" href="${ctx}/product?id=${item.productId}&amp;order=${order.id}">Leave a review</a>
                        </c:when>
                        <c:otherwise>
                            <span class="form-hint">Available after delivery</span>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<div class="cart-summary">
    <span class="cart-total">Total: ₹ <fmt:formatNumber value="${order.totalAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></span>
</div>
<p style="margin-top:16px;"><a href="${ctx}/orders" class="btn btn-secondary">Back to Order History</a></p>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
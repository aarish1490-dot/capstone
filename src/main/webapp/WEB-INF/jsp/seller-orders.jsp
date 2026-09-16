<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Seller Orders"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<h2 class="page-title">Seller Orders</h2>
<p class="page-subtitle">Orders that contain your products</p>

<c:if test="${param.msg == 'advanced'}">
    <div class="alert alert-success">Order status updated successfully.</div>
</c:if>
<c:if test="${param.msg == 'state'}">
    <div class="alert alert-error">This order cannot be advanced further or is in an invalid state.</div>
</c:if>
<c:if test="${param.msg == 'error'}">
    <div class="alert alert-error">Something went wrong. Please try again.</div>
</c:if>

<c:choose>
    <c:when test="${empty views}">
        <div class="empty-state">
            <h3>No orders yet</h3>
            <p>When a buyer orders one of your products, it will appear here.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap keep-table">
            <table class="seller-table">
                <thead>
                <tr>
                    <th>Order #</th>
                    <th>Date</th>
                    <th>Buyer</th>
                    <th>Items</th>
                    <th>Your Total</th>
                    <th>Status</th>
                    <th>Action</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="view" items="${views}">
                    <c:set var="order" value="${view.order}"/>
                    <c:set var="items" value="${view.items}"/>
                    <tr>
                        <td style="font-weight:700;">#<c:out value="${order.id}"/></td>
                        <td><fmt:formatDate value="${order.createdAt}" pattern="dd MMM yyyy, HH:mm"/></td>
                        <td><c:out value="${order.buyerName}"/></td>
                        <td>
                            <ul class="seller-order-items">
                                <c:forEach var="item" items="${items}">
                                    <li><c:out value="${item.quantity}"/> &times; <c:out value="${item.productName}"/></li>
                                </c:forEach>
                            </ul>
                        </td>
                        <td>
                            ₹ <fmt:formatNumber value="${view.subtotal}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
                        </td>
                        <td>
                            <span class="badge <c:choose>
                                <c:when test="${order.status == 'PENDING'}">badge-pending</c:when>
                                <c:when test="${order.status == 'CONFIRMED'}">badge-confirmed</c:when>
                                <c:when test="${order.status == 'SHIPPED'}">badge-shipped</c:when>
                                <c:when test="${order.status == 'DELIVERED'}">badge-delivered</c:when>
                                <c:otherwise>badge-pending</c:otherwise>
                            </c:choose>">
                                <c:out value="${order.status}"/>
                            </span>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${order.status == 'PENDING'}">
                                    <form action="${ctx}/seller/orders/status" method="post" style="display:inline;">
                                        <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                                        <input type="hidden" name="orderId" value="${order.id}"/>
                                        <button type="submit" class="btn btn-sm">Confirm</button>
                                    </form>
                                </c:when>
                                <c:when test="${order.status == 'CONFIRMED'}">
                                    <form action="${ctx}/seller/orders/status" method="post" style="display:inline;">
                                        <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                                        <input type="hidden" name="orderId" value="${order.id}"/>
                                        <button type="submit" class="btn btn-sm">Ship</button>
                                    </form>
                                </c:when>
                                <c:when test="${order.status == 'SHIPPED'}">
                                    <form action="${ctx}/seller/orders/status" method="post" style="display:inline;">
                                        <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                                        <input type="hidden" name="orderId" value="${order.id}"/>
                                        <button type="submit" class="btn btn-sm">Deliver</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <span class="form-hint">Complete</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
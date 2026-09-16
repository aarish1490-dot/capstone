<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Order History"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<h2 class="page-title">Order History</h2>
<p class="page-subtitle">All your orders at a glance</p>

<c:choose>
    <c:when test="${empty orders}">
        <div class="empty-state">
            <h3>No orders yet</h3>
            <p>When you place an order, it will appear here.</p>
            <a href="${ctx}/products" class="btn" style="margin-top:14px;">Start Shopping</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>Order ID</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Total</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="o" items="${orders}">
                    <tr>
                        <td style="font-weight:700;">#<c:out value="${o.id}"/></td>
                        <td><fmt:formatDate value="${o.createdAt}" pattern="dd MMM yyyy, HH:mm"/></td>
                        <td>
                            <span class="badge <c:choose>
                                <c:when test="${o.status == 'PENDING'}">badge-pending</c:when>
                                <c:when test="${o.status == 'CONFIRMED'}">badge-confirmed</c:when>
                                <c:when test="${o.status == 'SHIPPED'}">badge-shipped</c:when>
                                <c:when test="${o.status == 'DELIVERED'}">badge-delivered</c:when>
                                <c:otherwise>badge-pending</c:otherwise>
                            </c:choose>">
                                <c:out value="${o.status}"/>
                            </span>
                        </td>
                        <td style="font-weight:700;">₹ <fmt:formatNumber value="${o.totalAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                        <td><a class="btn btn-sm btn-secondary" href="${ctx}/order?id=${o.id}">View</a></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Checkout"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<h2 class="page-title">Checkout</h2>
<p class="page-subtitle">Review your order summary and confirm the mock payment</p>

<div class="table-wrap" style="margin-bottom:18px;">
    <table>
        <thead>
        <tr>
            <th>Product</th>
            <th>Quantity</th>
            <th>Unit Price</th>
            <th>Subtotal</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="line" items="${cart.lines}">
            <tr>
                <td><c:out value="${line.product.name}"/></td>
                <td><c:out value="${line.quantity}"/></td>
                <td>₹ <fmt:formatNumber value="${line.product.price}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                <td>₹ <fmt:formatNumber value="${line.subtotal}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<div class="cart-summary">
    <span class="form-hint">Items: <c:out value="${cart.count}"/></span>
    <span class="cart-total">Order Total: ₹ <fmt:formatNumber value="${cart.total}" type="number" minFractionDigits="2" maxFractionDigits="2"/></span>
</div>

<div style="margin-top:22px;background:#fff;border:1px solid var(--border);border-radius:var(--radius);padding:20px;">
    <p style="font-weight:700;margin-bottom:6px;">Mock Payment</p>
    <p class="form-hint" style="margin-bottom:14px;">
        This is a demonstration checkout. No real payment is processed. A confirmation will simulate the payment gateway.
    </p>
    <form action="${ctx}/checkout" method="post">
        <input type="hidden" name="_csrf" value="${_csrfToken}"/>
        <button type="submit" class="btn" style="padding:12px 28px;">Confirm Mock Payment</button>
        <a href="${ctx}/cart" class="btn btn-secondary">Back to Cart</a>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>

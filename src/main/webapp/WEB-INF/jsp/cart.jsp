<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Cart"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<h2 class="page-title">Your Cart</h2>
<p class="page-subtitle">Review your items, tweak quantities and head to checkout</p>

<c:if test="${not empty param.msg}">
    <div class="alert alert-warning" role="alert"><c:out value="${param.msg}"/></div>
</c:if>
<c:if test="${cart.hasWarnings}">
    <div class="alert alert-warning" role="alert">
        <c:if test="${cart.unavailableRemoved > 0}">
            <div><c:out value="${cart.unavailableRemoved}"/> item(s) in your cart are no longer available and were removed.</div>
        </c:if>
        <c:if test="${cart.quantityReduced > 0}">
            <div>Some quantities were adjusted to match what's actually in stock.</div>
        </c:if>
    </div>
</c:if>

<c:choose>
    <c:when test="${cart.isEmpty()}">
        <div class="empty-state cart-empty">
            <h3>Oops... your cart is feeling lonely.</h3>
            <p>Let's find something worth bringing home.</p>
            <a href="${ctx}/products" class="btn" style="margin-top:14px;">Start Shopping</a>
        </div>
    </c:when>
    <c:otherwise>
        <script>window.CONTEXT_PATH = "${ctx}";</script>
        <script src="${ctx}/js/cart.js"></script>
        <div class="cart-layout">
            <div class="cart-items">
                <c:forEach var="line" items="${cart.lines}">
                    <article class="cart-row"
                             data-product-id="${line.product.id}"
                             data-price="<c:out value='${line.product.price}'/>"
                             data-stock="${line.product.stockQty}">
                        <a class="cart-thumb-link" href="${ctx}/product?id=${line.product.id}" tabindex="-1" aria-hidden="true">
                            <img class="cart-thumb"
                                 src="<c:out value='${line.product.imageUrl}'/>"
                                 alt="<c:out value='${line.product.name}'/>"
                                 onerror="this.onerror=null;this.src='<c:url value="/images/placeholder.png"/>'">
                        </a>
                        <div class="cart-row-body">
                            <a class="cart-item-name" href="${ctx}/product?id=${line.product.id}">
                                <c:out value="${line.product.name}"/>
                            </a>
                            <span class="cart-item-seller">by <c:out value="${line.product.sellerName}"/></span>
                            <div class="cart-item-meta">
                                <span class="cart-unit-price">
                                    &#8377; <fmt:formatNumber value="${line.product.price}" type="number" minFractionDigits="2" maxFractionDigits="2"/>/unit
                                </span>
                                <span class="stock-pill
                                    <c:choose>
                                        <c:when test="${line.product.stockQty < 10}">low</c:when>
                                        <c:otherwise>in</c:otherwise>
                                    </c:choose>">
                                    <c:choose>
                                        <c:when test="${line.product.stockQty < 10}">Only <c:out value="${line.product.stockQty}"/> left</c:when>
                                        <c:otherwise>In stock</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </div>
                        <div class="cart-qty">
                            <button type="button" class="qty-btn qty-minus" aria-label="Decrease quantity"
                                    <c:if test="${line.quantity <= 1}">disabled</c:if>>&minus;</button>
                            <input class="qty-input" type="text" inputmode="numeric" value="${line.quantity}"
                                   readonly aria-label="Quantity">
                            <button type="button" class="qty-btn qty-plus" aria-label="Increase quantity"
                                    <c:if test="${line.quantity >= line.product.stockQty}">disabled</c:if>>+</button>
                        </div>
                        <div class="cart-row-total">
                            <span class="subtotal">&#8377;
                                <fmt:formatNumber value="${line.subtotal}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
                            </span>
                        </div>
                        <div class="cart-row-actions">
                            <button type="button" class="btn btn-sm btn-danger remove-item"
                                    data-product-id="${line.product.id}">Remove</button>
                        </div>
                    </article>
                </c:forEach>
            </div>
            <aside class="summary-card" aria-label="Order summary">
                <h3 class="summary-title">Order Summary</h3>
                <div class="summary-line">
                    <span>Total items</span>
                    <span id="summary-count"><c:out value="${cart.count}"/></span>
                </div>
                <div class="summary-line">
                    <span>Subtotal</span>
                    <span>&#8377; <fmt:formatNumber value="${cart.total}" type="number" minFractionDigits="2" maxFractionDigits="2"/></span>
                </div>
                <div class="summary-total">
                    <span>Total</span>
                    <span id="cart-total">&#8377; <fmt:formatNumber value="${cart.total}" type="number" minFractionDigits="2" maxFractionDigits="2"/></span>
                </div>
                <a href="${ctx}/checkout" class="btn cart-checkout">Proceed to Checkout</a>
                <a href="${ctx}/products" class="btn btn-secondary cart-continue">Continue Shopping</a>
                <p class="summary-note">Shipping and taxes are calculated at checkout.</p>
            </aside>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
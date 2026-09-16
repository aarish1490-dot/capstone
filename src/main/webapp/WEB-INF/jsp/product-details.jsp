<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="${product.name}"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<div class="detail-layout">
    <div>
        <img class="detail-img" src="<c:out value='${product.imageUrl}'/>"
             alt="<c:out value='${product.name}'/>"
             onerror="this.src='${ctx}/images/placeholder.png'">
    </div>
    <div class="detail-info">
        <div class="card-meta" style="margin-bottom:6px;">
            <span class="badge" style="background:var(--light);"><c:out value="${product.category}"/></span>
            <span>Sold by <c:out value="${product.sellerName}"/></span>
        </div>
        <h1><c:out value="${product.name}"/></h1>
        <div class="price" style="font-size:1.5rem;">
            ₹ <fmt:formatNumber value="${product.price}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
        </div>
        <div class="form-hint">
            <c:choose>
                <c:when test="${product.stockQty == 0}">
                    <span class="stock-out"><strong>Out of stock</strong></span>
                </c:when>
                <c:when test="${product.stockQty < 10}">
                    <span class="stock-low"><strong>Only <c:out value="${product.stockQty}"/> left in stock</strong></span>
                </c:when>
                <c:otherwise>
                    <span class="stock-ok"><strong>In stock (<c:out value="${product.stockQty}"/> available)</strong></span>
                </c:otherwise>
            </c:choose>
        </div>
        <p class="detail-desc"><c:out value="${product.description}"/></p>

        <c:if test="${product.stockQty > 0}">
            <form action="${ctx}/cart" method="post">
                <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="productId" value="${product.id}">
                <div class="qty-row">
                    <label for="qty" style="font-weight:600;">Quantity</label>
                    <input class="qty-input" type="number" id="qty" name="quantity" value="1"
                           min="1" max="${product.stockQty}">
                </div>
                <button type="submit" class="btn">Add to Cart</button>
                <a href="${ctx}/products" class="btn btn-secondary">Back to Products</a>
            </form>
        </c:if>
        <c:if test="${product.stockQty == 0}">
            <a href="${ctx}/products" class="btn btn-secondary">Back to Products</a>
        </c:if>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>

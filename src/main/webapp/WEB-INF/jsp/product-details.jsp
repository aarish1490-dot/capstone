<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="${product.name}"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="${ctx}/">Home</a>
    <span aria-hidden="true">&rsaquo;</span>
    <a href="${ctx}/products">Products</a>
    <span aria-hidden="true">&rsaquo;</span>
    <span aria-current="page"><c:out value="${product.name}"/></span>
</nav>

<div class="detail-layout">
    <div class="detail-media">
        <img class="detail-img" src="<c:out value='${product.imageUrl}'/>"
             alt="<c:out value='${product.name}'/>"
             onerror="this.onerror=null;this.src='<c:url value="/images/placeholder.png"/>'">
    </div>
    <div class="detail-info">
        <div class="card-topline">
            <span class="category-tag"><c:out value="${product.category}"/></span>
            <span class="stock-pill
                <c:choose>
                    <c:when test="${product.stockQty <= 0}">out</c:when>
                    <c:when test="${product.stockQty < 10}">low</c:when>
                    <c:otherwise>in</c:otherwise>
                </c:choose>">
                <c:choose>
                    <c:when test="${product.stockQty <= 0}">Out of stock</c:when>
                    <c:when test="${product.stockQty < 10}">Low stock</c:when>
                    <c:otherwise>In stock</c:otherwise>
                </c:choose>
            </span>
        </div>
        <h1><c:out value="${product.name}"/></h1>
        <p class="detail-seller">Sold by <c:out value="${product.sellerName}"/></p>
        <div class="price detail-price">
            &#8377; <fmt:formatNumber value="${product.price}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
        </div>
        <p class="detail-desc"><c:out value="${product.description}"/></p>

        <c:choose>
            <c:when test="${product.stockQty <= 0}">
                <p class="form-hint"><c:out value="${product.name}"/> is currently out of stock. Please check back later.</p>
                <a href="${ctx}/products" class="btn btn-secondary">Back to Products</a>
            </c:when>
            <c:when test="${empty sessionScope.user}">
                <p class="form-hint">Please <a href="${ctx}/login">log in</a> to add items to your cart.</p>
                <a href="${ctx}/login" class="btn">Login to Buy</a>
                <a href="${ctx}/products" class="btn btn-secondary">Back to Products</a>
            </c:when>
            <c:otherwise>
                <form class="add-to-cart-form detail-form" action="${ctx}/cart" method="post">
                    <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                    <input type="hidden" name="action" value="add"/>
                    <input type="hidden" name="productId" value="${product.id}"/>
                    <div class="qty-row">
                        <label for="qty" class="qty-label">Quantity</label>
                        <input class="qty-input" type="number" id="qty" name="quantity" value="1"
                               min="1" max="${product.stockQty}" step="1">
                    </div>
                    <div class="detail-actions">
                        <button type="submit" class="btn">Add to Cart</button>
                        <a href="${ctx}/products" class="btn btn-secondary">Back to Products</a>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
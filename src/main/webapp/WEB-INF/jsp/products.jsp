<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Products"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<h2 class="page-title">Explore Products</h2>
<p class="page-subtitle">Search by name, keyword or filter by category</p>

<form class="search-bar" action="${ctx}/products" method="get">
    <input type="text" name="q" placeholder="Search products..."
           value="<c:out value='${keyword}'/>">
    <select name="category">
        <option value="">All Categories</option>
        <c:forEach var="cat" items="${categories}">
            <option value="<c:out value='${cat}'/>"
                <c:if test="${cat == selectedCategory}">selected</c:if>>
                <c:out value="${cat}"/>
            </option>
        </c:forEach>
    </select>
    <button type="submit" class="btn">Search</button>
    <a href="${ctx}/products" class="btn btn-secondary">Reset</a>
</form>

<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>

<c:choose>
    <c:when test="${empty products}">
        <div class="empty-state">
            <h3>No products found</h3>
            <p>Try a different search term or category.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="grid-products">
            <c:forEach var="p" items="${products}">
                <div class="card">
                    <img class="card-img" src="<c:out value='${p.imageUrl}'/>"
                         alt="<c:out value='${p.name}'/>" loading="lazy"
                         onerror="this.src='${ctx}/images/placeholder.png'">
                    <div class="card-body">
                        <a class="card-title" href="${ctx}/product?id=${p.id}"><c:out value="${p.name}"/></a>
                        <div class="card-meta">
                            <span><c:out value="${p.category}"/></span>
                            <span class="price">₹ <fmt:formatNumber value="${p.price}" type="number" minFractionDigits="2" maxFractionDigits="2"/></span>
                        </div>
                        <div class="card-meta">
                            <span class="<c:choose><c:when test='${p.stockQty == 0}'>stock-out</c:when><c:when test='${p.stockQty < 10}'>stock-low</c:when><c:otherwise>stock-ok</c:otherwise></c:choose>">
                                <c:choose>
                                    <c:when test="${p.stockQty == 0}">Out of stock</c:when>
                                    <c:otherwise>In stock: <c:out value="${p.stockQty}"/></c:otherwise>
                                </c:choose>
                            </span>
                            <span>by <c:out value="${p.sellerName}"/></span>
                        </div>
                        <div class="card-actions">
                            <a class="btn btn-sm" href="${ctx}/product?id=${p.id}">View</a>
                            <c:choose>
                                <c:when test="${p.stockQty > 0}">
                                    <form action="${ctx}/cart" method="post" style="flex:1;">
                                        <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                                        <input type="hidden" name="action" value="add">
                                        <input type="hidden" name="productId" value="${p.id}">
                                        <input type="hidden" name="quantity" value="1">
                                        <button type="submit" class="btn btn-sm" style="width:100%;">Add to Cart</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <button class="btn btn-sm" style="width:100%;" disabled>Out of stock</button>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Welcome"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<div class="hero">
    <h1>Discover something worth bringing home.</h1>
    <p>Browse products from verified sellers, search across the catalogue, and add favourites to your cart in
        seconds.</p>
    <form class="hero-search" action="${ctx}/products" method="get">
        <label class="sr-only" for="hero-q">Search the marketplace</label>
        <input type="text" id="hero-q" name="q" placeholder="Search the marketplace...">
        <button type="submit" class="btn">Search</button>
    </form>
    <div class="hero-actions">
        <a href="${ctx}/products" class="btn">Browse all products</a>
        <c:if test="${empty sessionScope.user}">
            <a href="${ctx}/register" class="btn btn-secondary">Create Account</a>
        </c:if>
    </div>
</div>

<h2 class="page-title">Shop by category</h2>
<p class="page-subtitle">Find exactly what you are looking for</p>
<div class="category-chips">
    <a href="${ctx}/products" class="category-chip">All Products</a>
    <c:forEach var="cat" items="${categories}">
        <c:url var="catUrl" value="/products">
            <c:param name="category" value="${cat}"/>
        </c:url>
        <a href="${catUrl}" class="category-chip"><c:out value="${cat}"/></a>
    </c:forEach>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>

<section aria-labelledby="featured-title">
    <h2 class="page-title" id="featured-title">Fresh on the shelves</h2>
    <p class="page-subtitle">Recently listed products from our sellers</p>
    <c:choose>
        <c:when test="${not empty featuredProducts}">
            <div class="grid-products">
                <c:forEach var="p" items="${featuredProducts}">
                    <%@ include file="/WEB-INF/jsp/fragments/product-card.jspf" %>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise>
            <div class="empty-state">
                <h3>No products listed yet</h3>
                <p>Our sellers are stocking the shelves. Please check back soon.</p>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<div class="stat-grid">
    <div class="stat-card">
        <div class="stat-value">Multi-seller</div>
        <div class="stat-label">Verified sellers list their products</div>
    </div>
    <div class="stat-card">
        <div class="stat-value">Search &amp; filter</div>
        <div class="stat-label">Find exactly what you need, fast</div>
    </div>
    <div class="stat-card">
        <div class="stat-value">Secure</div>
        <div class="stat-label">Checkout &amp; order tracking</div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
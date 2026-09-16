<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Products"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<h2 class="page-title">Explore Products</h2>
<p class="page-subtitle">Search by name or keyword, or filter by category</p>

<form class="search-bar" action="${ctx}/products" method="get">
    <div class="search-field">
        <label class="sr-only" for="search-q">Search products</label>
        <input type="text" id="search-q" name="q" placeholder="Search products..."
               value="<c:out value='${keyword}'/>">
    </div>
    <div class="search-field">
        <label class="sr-only" for="search-category">Category</label>
        <select id="search-category" name="category">
            <option value="">All Categories</option>
            <c:forEach var="cat" items="${categories}">
                <option value="<c:out value='${cat}'/>"
                    <c:if test="${cat == selectedCategory}">selected</c:if>>
                    <c:out value="${cat}"/>
                </option>
            </c:forEach>
        </select>
    </div>
    <button type="submit" class="btn">Search</button>
    <a href="${ctx}/products" class="btn btn-secondary">Reset</a>
</form>

<c:if test="${not empty error}">
    <div class="alert alert-error"><c:out value="${error}"/></div>
</c:if>

<c:choose>
    <c:when test="${empty products}">
        <div class="empty-state">
            <h3 id="empty-title">Looks like the shelves are taking a nap.</h3>
            <p>
                <c:choose>
                    <c:when test="${not empty keyword || not empty selectedCategory}">
                        No products matched
                        <c:if test="${not empty keyword}">“<c:out value="${keyword}"/>”</c:if>
                        <c:if test="${not empty keyword && not empty selectedCategory}"> in </c:if>
                        <c:if test="${not empty selectedCategory}"><c:out value="${selectedCategory}"/></c:if>.
                        Try a different search term or category, or clear your filters.
                    </c:when>
                    <c:otherwise>
                        We couldn't find any products right now. Please check back soon.
                    </c:otherwise>
                </c:choose>
            </p>
            <a href="${ctx}/products" class="btn">Clear Filters</a>
        </div>
    </c:when>
    <c:otherwise>
        <c:set var="resultsStart" value="${(page.page - 1) * page.pageSize + 1}"/>
        <c:set var="resultsEnd" value="${resultsStart + page.products.size() - 1}"/>
        <div class="results-meta" role="status" aria-live="polite">
            <span>Showing <c:out value="${resultsStart}"/>&ndash;<c:out value="${resultsEnd}"/> of <c:out value="${page.totalItems}"/> products</span>
            <c:if test="${not empty keyword}">
                <span class="filter-chip">Keyword: <c:out value="${keyword}"/></span>
            </c:if>
            <c:if test="${not empty selectedCategory}">
                <span class="filter-chip">Category: <c:out value="${selectedCategory}"/></span>
            </c:if>
        </div>
        <div class="grid-products">
            <c:forEach var="p" items="${products}">
                <%@ include file="/WEB-INF/jsp/fragments/product-card.jspf" %>
            </c:forEach>
        </div>

        <c:if test="${page.totalPages > 1}">
            <nav class="pagination" aria-label="Product pages">
                <c:choose>
                    <c:when test="${page.hasPrevious}">
                        <c:url var="prevUrl" value="/products">
                            <c:param name="q" value="${keyword}"/>
                            <c:param name="category" value="${selectedCategory}"/>
                            <c:param name="page" value="${page.page - 1}"/>
                        </c:url>
                        <a class="btn btn-secondary btn-sm" href="${prevUrl}" rel="prev">Previous</a>
                    </c:when>
                    <c:otherwise>
                        <button class="btn btn-disabled btn-sm" type="button" disabled>Previous</button>
                    </c:otherwise>
                </c:choose>
                <span class="pagination-info">Page <c:out value="${page.page}"/> of <c:out value="${page.totalPages}"/></span>
                <c:choose>
                    <c:when test="${page.hasNext}">
                        <c:url var="nextUrl" value="/products">
                            <c:param name="q" value="${keyword}"/>
                            <c:param name="category" value="${selectedCategory}"/>
                            <c:param name="page" value="${page.page + 1}"/>
                        </c:url>
                        <a class="btn btn-secondary btn-sm" href="${nextUrl}" rel="next">Next</a>
                    </c:when>
                    <c:otherwise>
                        <button class="btn btn-disabled btn-sm" type="button" disabled>Next</button>
                    </c:otherwise>
                </c:choose>
            </nav>
        </c:if>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
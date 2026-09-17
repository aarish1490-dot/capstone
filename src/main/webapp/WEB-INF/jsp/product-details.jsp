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
        <div class="rating-summary">
            <c:choose>
                <c:when test="${reviewStats.count > 0}">
                    <span class="stars" aria-hidden="true">
                        <c:forEach begin="1" end="${reviewStats.fullStars}">&#9733;</c:forEach>
                        <c:forEach begin="${reviewStats.fullStars + 1}" end="5">&#9734;</c:forEach>
                    </span>
                    <span class="rating-value">
                        <strong><fmt:formatNumber value="${reviewStats.average}" pattern="0.0"/></strong>
                        &middot; <c:out value="${reviewStats.count}"/> review<c:if test="${reviewStats.count != 1}">s</c:if>
                    </span>
                </c:when>
                <c:otherwise>
                    <span class="rating-value">No reviews yet</span>
                </c:otherwise>
            </c:choose>
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

<c:if test="${not empty error}">
    <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
</c:if>
<c:if test="${param.reviewed == '1'}">
    <div class="alert alert-success" role="alert">Thanks! Your review has been added.</div>
</c:if>

<section class="reviews-section" aria-label="Customer reviews">
    <h2 class="section-title">Customer Reviews</h2>

    <c:choose>
        <c:when test="${reviewStats.count > 0}">
            <p class="review-count-note">
            <span class="stars" aria-hidden="true">
                <c:forEach begin="1" end="${reviewStats.fullStars}">&#9733;</c:forEach>
                <c:forEach begin="${reviewStats.fullStars + 1}" end="5">&#9734;</c:forEach>
            </span>
            <strong><fmt:formatNumber value="${reviewStats.average}" pattern="0.0"/></strong>
            &middot; rated by <c:out value="${reviewStats.count}"/> customer<c:if test="${reviewStats.count != 1}">s</c:if>
            </p>
        </c:when>
        <c:otherwise>
            <p class="form-hint">No reviews yet. Be the first to share your experience.</p>
        </c:otherwise>
    </c:choose>

    <div class="review-list">
        <c:forEach var="review" items="${reviews}">
            <article class="review-card">
                <div class="review-meta">
                    <span class="review-author"><c:out value="${review.buyerName}"/></span>
                    <span class="stars stars-sm" aria-hidden="true">
                        <c:forEach begin="1" end="${review.rating}">&#9733;</c:forEach>
                        <c:forEach begin="${review.rating + 1}" end="5">&#9734;</c:forEach>
                    </span>
                    <span class="review-date"><fmt:formatDate value="${review.createdAt}" pattern="dd MMM yyyy"/></span>
                </div>
                <p class="review-text"><c:out value="${review.reviewText}"/></p>
            </article>
        </c:forEach>
    </div>

    <c:choose>
        <c:when test="${empty sessionScope.user}">
            <p class="review-login-prompt">Please <a href="${ctx}/login">log in</a> to share your experience.</p>
        </c:when>
        <c:when test="${sessionScope.user.role != 'BUYER'}">
            <p class="form-hint">Reviews are written by verified buyers after their order is delivered.</p>
        </c:when>
        <c:when test="${not empty myReview and empty eligibleOrderId}">
            <p class="my-review-note">
                You rated this product
                <span class="stars stars-sm" aria-hidden="true">
                    <c:forEach begin="1" end="${myReview.rating}">&#9733;</c:forEach>
                    <c:forEach begin="${myReview.rating + 1}" end="5">&#9734;</c:forEach>
                </span>
                &middot; thanks!
            </p>
        </c:when>
        <c:when test="${not empty eligibleOrderId}">
            <div class="review-form-card">
                <h3>Write a Review</h3>
                <form class="review-form" action="${ctx}/review" method="post">
                    <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                    <input type="hidden" name="productId" value="${product.id}"/>
                    <input type="hidden" name="orderId" value="${eligibleOrderId}"/>
                    <c:set var="selectedRating" value="${empty param.rating ? 5 : param.rating}"/>
                    <fieldset class="star-selector">
                        <legend>Your rating</legend>
                        <c:forEach var="star" begin="1" end="5">
                            <label class="star-option">
                                <input type="radio" name="rating" value="${star}"
                                       <c:if test="${selectedRating == star}">checked</c:if> required>
                                <span class="star-char" aria-hidden="true">&#9733;</span>
                                <span class="sr-only">${star} star${star != 1 ? 's' : ''}</span>
                            </label>
                        </c:forEach>
                    </fieldset>
                    <div class="form-group">
                        <label for="reviewText">Share your experience</label>
                        <textarea id="reviewText" name="reviewText" rows="4" maxlength="500"
                                  placeholder="What did you like or dislike about this product?"><c:out value="${param.reviewText}"/></textarea>
                        <p class="form-hint">Up to 500 characters.</p>
                    </div>
                    <button type="submit" class="btn">Submit Review</button>
                </form>
            </div>
        </c:when>
    </c:choose>
</section>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
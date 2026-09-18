<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Admin Dashboard"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<h2 class="page-title">Admin Dashboard</h2>
<p class="page-subtitle">Platform overview, accounts, orders and catalog moderation</p>

<c:if test="${param.msg == 'user'}">
    <div class="alert alert-success">User account status updated.</div>
</c:if>
<c:if test="${param.msg == 'product'}">
    <div class="alert alert-success">Product listing status updated.</div>
</c:if>

<div class="stat-grid">
    <div class="stat-card">
        <div class="stat-value"><c:out value="${stats.totalUsers}"/></div>
        <div class="stat-label">Total Users</div>
    </div>
    <div class="stat-card">
        <div class="stat-value"><c:out value="${stats.totalBuyers}"/></div>
        <div class="stat-label">Buyers</div>
    </div>
    <div class="stat-card">
        <div class="stat-value"><c:out value="${stats.totalSellers}"/></div>
        <div class="stat-label">Sellers</div>
    </div>
    <div class="stat-card">
        <div class="stat-value"><c:out value="${stats.totalProducts}"/></div>
        <div class="stat-label">Total Products</div>
    </div>
    <div class="stat-card">
        <div class="stat-value"><c:out value="${stats.totalOrders}"/></div>
        <div class="stat-label">Total Orders</div>
    </div>
</div>

<div class="dashboard-section">
    <h3>Users</h3>
    <p class="section-note">Deactivated accounts can no longer sign in with email or OTP.</p>
    <div class="table-wrap keep-table">
        <table class="seller-table">
            <thead>
            <tr>
                <th>User</th>
                <th>Email</th>
                <th>Mobile</th>
                <th>Role</th>
                <th>Status</th>
                <th>Action</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="u" items="${users}">
                <tr>
                    <td class="name-cell"><c:out value="${u.name}"/></td>
                    <td data-label="Email"><c:out value="${u.email}"/></td>
                    <td data-label="Mobile"><c:out value="${u.mobileNumber}"/></td>
                    <td data-label="Role"><c:out value="${u.role}"/></td>
                    <td data-label="Status">
                        <c:choose>
                            <c:when test="${u.active}">
                                <span class="badge badge-ok">Active</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-out">Inactive</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="actions-cell" data-label="Action">
                        <c:choose>
                            <c:when test="${u.id == sessionScope.user.id}">
                                <span class="form-hint">This is you</span>
                            </c:when>
                            <c:when test="${u.role.name() == 'ADMIN'}">
                                <span class="form-hint">Protected</span>
                            </c:when>
                            <c:otherwise>
                                <form action="${ctx}/admin" method="post" class="delete-form">
                                    <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                                    <input type="hidden" name="action" value="user-active"/>
                                    <input type="hidden" name="userId" value="${u.id}"/>
                                    <input type="hidden" name="active" value="${u.active ? 'false' : 'true'}"/>
                                    <c:choose>
                                        <c:when test="${u.active}">
                                            <button type="submit" class="btn btn-sm btn-danger">Deactivate</button>
                                        </c:when>
                                        <c:otherwise>
                                            <button type="submit" class="btn btn-sm">Activate</button>
                                        </c:otherwise>
                                    </c:choose>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<div class="dashboard-section">
    <h3>Orders</h3>
    <p class="section-note">All orders across the platform, newest first.</p>
    <c:choose>
        <c:when test="${empty orders}">
            <div class="empty-state">
                <h3>No orders yet</h3>
                <p>Orders will appear here as buyers check out.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap keep-table">
                <table class="seller-table">
                    <thead>
                    <tr>
                        <th>Order</th>
                        <th>Buyer</th>
                        <th>Status</th>
                        <th>Total</th>
                        <th>Date</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="o" items="${orders}">
                        <tr>
                            <td class="name-cell">#<c:out value="${o.id}"/></td>
                            <td data-label="Buyer"><c:out value="${o.buyerName}"/></td>
                            <td data-label="Status">
                                <c:choose>
                                    <c:when test="${o.status == 'PENDING'}">
                                        <span class="badge badge-pending">Pending</span>
                                    </c:when>
                                    <c:when test="${o.status == 'CONFIRMED'}">
                                        <span class="badge badge-confirmed">Confirmed</span>
                                    </c:when>
                                    <c:when test="${o.status == 'SHIPPED'}">
                                        <span class="badge badge-shipped">Shipped</span>
                                    </c:when>
                                    <c:when test="${o.status == 'DELIVERED'}">
                                        <span class="badge badge-delivered">Delivered</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-low"><c:out value="${o.status}"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="price-cell" data-label="Total">₹
                                <fmt:formatNumber value="${o.totalAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
                            </td>
                            <td data-label="Date">
                                <fmt:formatDate value="${o.createdAt}" pattern="dd MMM yyyy, hh:mm a"/>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="dashboard-section">
    <h3>Product Moderation</h3>
    <p class="section-note">Unlisted products are hidden from buyers, but remain visible to their sellers.</p>
    <c:choose>
        <c:when test="${empty products}">
            <div class="empty-state">
                <h3>No products yet</h3>
                <p>Sellers have not added any products.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap keep-table">
                <table class="seller-table">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th>Seller</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${products}">
                        <tr>
                            <td class="name-cell">
                                <img class="thumb" src="<c:out value='${p.imageUrl}'/>" alt="<c:out value='${p.name}'/>"
                                     onerror="this.onerror=null;this.src='${ctx}/images/placeholder.png'">
                                <span><c:out value="${p.name}"/></span>
                            </td>
                            <td data-label="Seller"><c:out value="${p.sellerName}"/></td>
                            <td data-label="Category"><c:out value="${p.category}"/></td>
                            <td class="price-cell" data-label="Price">₹
                                <fmt:formatNumber value="${p.price}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
                            </td>
                            <td data-label="Stock"><c:out value="${p.stockQty}"/></td>
                            <td data-label="Status">
                                <c:choose>
                                    <c:when test="${p.active}">
                                        <span class="badge badge-ok">Listed</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-out">Unlisted</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="actions-cell" data-label="Action">
                                <form action="${ctx}/admin" method="post" class="delete-form">
                                    <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                                    <input type="hidden" name="action" value="product-active"/>
                                    <input type="hidden" name="productId" value="${p.id}"/>
                                    <input type="hidden" name="active" value="${p.active ? 'false' : 'true'}"/>
                                    <c:choose>
                                        <c:when test="${p.active}">
                                            <button type="submit" class="btn btn-sm btn-danger">Unlist</button>
                                        </c:when>
                                        <c:otherwise>
                                            <button type="submit" class="btn btn-sm">Relist</button>
                                        </c:otherwise>
                                    </c:choose>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="title" value="Seller Dashboard"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<div class="seller-header">
    <div>
        <h2 class="page-title">Seller Dashboard</h2>
        <p class="page-subtitle">Welcome back, <c:out value="${sessionScope.user.name}"/>. Manage your products from here.</p>
    </div>
    <a href="${ctx}/seller/product/create" class="btn">+ Add Product</a>
</div>

<c:if test="${param.msg == 'created'}">
    <div class="alert alert-success">Product created and is now live in the marketplace.</div>
</c:if>
<c:if test="${param.msg == 'updated'}">
    <div class="alert alert-success">Product updated successfully.</div>
</c:if>
<c:if test="${param.msg == 'deleted'}">
    <div class="alert alert-success">Product deleted.</div>
</c:if>
<c:if test="${param.err == 'delete'}">
    <div class="alert alert-error">The product could not be deleted. It may be part of an existing order.</div>
</c:if>

<div class="stat-grid">
    <div class="stat-card">
        <div class="stat-value"><c:out value="${sales.orderCount}"/></div>
        <div class="stat-label">Orders Received</div>
    </div>
    <div class="stat-card">
        <div class="stat-value"><c:out value="${sales.unitsSold}"/></div>
        <div class="stat-label">Units Sold</div>
    </div>
    <div class="stat-card">
        <div class="stat-value">₹
            <fmt:formatNumber value="${sales.revenue}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
        </div>
        <div class="stat-label">Revenue</div>
    </div>
</div>

<div class="dashboard-section">
    <h3>Your Products</h3>
    <p class="section-note">
        <c:out value="${productCount}"/> product<c:if test="${productCount != 1}">s</c:if> currently listed
    </p>

    <c:choose>
        <c:when test="${empty products}">
            <div class="empty-state">
                <h3>Your shelves are looking a little empty</h3>
                <p>Add your first product to get things moving.</p>
                <a href="${ctx}/seller/product/create" class="btn" style="margin-top:14px;">Add your first product</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap keep-table">
                <table class="seller-table">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Actions</th>
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
                            <td data-label="Category"><c:out value="${p.category}"/></td>
                            <td class="price-cell" data-label="Price">₹
                                <fmt:formatNumber value="${p.price}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
                            </td>
                            <td class="stock-cell" data-label="Stock">
                                <c:choose>
                                    <c:when test="${p.stockQty == 0}">
                                        <span class="badge badge-out">Out of stock</span>
                                    </c:when>
                                    <c:when test="${p.stockQty < 10}">
                                        <span class="badge badge-low">Low &middot; <c:out value="${p.stockQty}"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-ok">In stock &middot; <c:out value="${p.stockQty}"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="actions-cell">
                                <div class="seller-actions">
                                    <a class="btn btn-sm btn-secondary" href="${ctx}/seller/product/edit?id=${p.id}">Edit</a>
                                    <button type="button" class="btn btn-sm btn-danger"
                                            data-delete-id="${p.id}" data-delete-name="<c:out value='${p.name}'/>">Delete</button>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="modal-overlay" id="deleteModal">
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="deleteModalTitle">
        <h3 id="deleteModalTitle">Delete this product?</h3>
        <p id="deleteModalText">"<span id="deleteProductName"></span>" will be removed from your listings.</p>
        <form action="${ctx}/seller/product/delete" method="post" id="deleteForm">
            <input type="hidden" name="_csrf" value="${_csrfToken}"/>
            <input type="hidden" name="id" id="deleteProductId"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" id="deleteCancel">Cancel</button>
                <button type="submit" class="btn btn-danger">Delete</button>
            </div>
        </form>
    </div>
</div>

<script>
(function () {
    var modal = document.getElementById('deleteModal');
    var modalText = document.getElementById('deleteProductName');
    var deleteId = document.getElementById('deleteProductId');
    var cancel = document.getElementById('deleteCancel');
    var activeDelete = null;

    function closeModal() {
        modal.classList.remove('open');
        if (activeDelete) {
            activeDelete.focus();
            activeDelete = null;
        }
    }

    document.addEventListener('click', function (e) {
        var trigger = e.target.closest('button[data-delete-id]');
        if (!trigger) {
            return;
        }
        activeDelete = trigger;
        deleteId.value = trigger.getAttribute('data-delete-id');
        modalText.textContent = trigger.getAttribute('data-delete-name');
        modal.classList.add('open');
        cancel.focus();
    });

    cancel.addEventListener('click', closeModal);

    modal.addEventListener('click', function (e) {
        if (e.target === modal) {
            closeModal();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && modal.classList.contains('open')) {
            closeModal();
        }
    });
})();
</script>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
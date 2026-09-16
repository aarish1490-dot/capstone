<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Edit Product"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<div class="form-card" style="max-width:620px;">
    <h2>Edit Product</h2>
    <p class="form-hint" style="margin-bottom:18px;">Update the details below and save your changes.</p>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <form action="${ctx}/seller/product/edit" method="post">
        <input type="hidden" name="_csrf" value="${_csrfToken}"/>
        <input type="hidden" name="id" value="${product.id}"/>
        <div class="form-group">
            <label for="name">Product Name</label>
            <input type="text" id="name" name="name" required maxlength="200"
                   placeholder="e.g. Handcrafted Leather Wallet"
                   value="<c:out value='${name}'/>">
        </div>
        <div class="form-group">
            <label for="description">Description</label>
            <textarea id="description" name="description" maxlength="4000"
                      placeholder="Describe what makes this product special"><c:out value="${description}"/></textarea>
        </div>
        <div class="form-group">
            <label for="price">Price (₹)</label>
            <input type="number" id="price" name="price" required step="0.01" min="0.01"
                   placeholder="e.g. 499.00"
                   value="<c:out value='${price}'/>">
            <div class="form-hint">Must be greater than zero. Maximum 2 decimal places.</div>
        </div>
        <div class="form-group">
            <label for="stock">Stock Quantity</label>
            <input type="number" id="stock" name="stock" required step="1" min="0"
                   placeholder="e.g. 25"
                   value="<c:out value='${stock}'/>">
            <div class="form-hint">Zero means the product shows as out of stock.</div>
        </div>
        <div class="form-group">
            <label for="category">Category</label>
            <input type="text" id="category" name="category" required maxlength="50"
                   list="category-list" placeholder="e.g. Home"
                   value="<c:out value='${category}'/>">
            <datalist id="category-list">
                <c:forEach var="cat" items="${categories}">
                    <option value="<c:out value='${cat}'/>">
                </c:forEach>
            </datalist>
            <div class="form-hint">Choose an existing category or type a new one</div>
        </div>
        <div class="form-group">
            <label for="imageUrl">Image URL</label>
            <input type="url" id="imageUrl" name="imageUrl" placeholder="https://..." maxlength="500"
                   value="<c:out value='${imageUrl}'/>">
            <div class="form-hint">Must start with http:// or https://</div>
        </div>
        <button type="submit" class="btn" style="width:100%;">Save Changes</button>
        <a href="${ctx}/seller" class="btn btn-secondary" style="width:100%;margin-top:10px;text-align:center;">Cancel</a>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dto.ProductPage;
import com.dhatchina.dhatchinamart.exception.ForbiddenException;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.util.ValidationUtil;

import java.util.List;

public class ProductService {

    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final int MAX_PAGE_SIZE = 48;
    private static final int MAX_CATEGORY_LENGTH = 50;

    private final ProductDAO productDAO;

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public List<Product> browse(String keyword, String category) {
        return productDAO.find(keyword, category);
    }

    /**
     * Returns a single page of products matching an optional keyword and
     * category. Keyword and category are trimmed (blank values become null)
     * and the requested page is clamped to a valid range.
     */
    public ProductPage browse(String keyword, String category, int page, int pageSize) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);
        int safePageSize = pageSize < 1 || pageSize > MAX_PAGE_SIZE ? DEFAULT_PAGE_SIZE : pageSize;
        int safePage = Math.max(page, 1);

        long totalItems = productDAO.count(normalizedKeyword, normalizedCategory);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safePageSize));
        int clampedPage = Math.min(safePage, totalPages);
        long offset = (long) (clampedPage - 1) * safePageSize;

        List<Product> products = productDAO.find(normalizedKeyword, normalizedCategory, safePageSize, offset);
        return new ProductPage(products, totalItems, clampedPage, safePageSize, totalPages);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public List<String> categories() {
        return productDAO.findCategories();
    }

    /**
     * Loads a product for buyers. Out-of-stock and deactivated products are
     * hidden from the marketplace and treated as not found here.
     */
    public Product getById(long id) {
        return productDAO.findById(id)
                .filter(product -> product.getStockQty() > 0 && product.isActive())
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    public List<Product> productsBySeller(long sellerId) {
        return productDAO.findBySeller(sellerId);
    }

    public long countBySeller(long sellerId) {
        return productDAO.countBySeller(sellerId);
    }

    /**
     * Loads a product and verifies the given seller owns it. Used by the
     * edit / delete flows - the seller is always taken from the session,
     * never from a browser-supplied value.
     */
    public Product getOwnedProduct(long sellerId, long productId) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (product.getSellerId() != sellerId) {
            throw new ForbiddenException("You do not have permission to manage this product");
        }
        return product;
    }

    /**
     * Validates the submitted values and updates an owned product.
     */
    public Product updateProduct(long sellerId, long productId, String name, String description,
                                 String price, String stock, String category, String imageUrl) {
        getOwnedProduct(sellerId, productId);

        Product product = new Product();
        product.setId(productId);
        product.setSellerId(sellerId);
        product.setName(ValidationUtil.requireProductName(name));
        product.setDescription(ValidationUtil.optionalText(description));
        product.setPrice(ValidationUtil.parsePrice(price));
        product.setStockQty(ValidationUtil.parseNonNegativeInt(stock, "Stock"));
        String cat = ValidationUtil.optionalText(category, MAX_CATEGORY_LENGTH);
        if (cat == null) {
            throw new ValidationException("Category is required");
        }
        product.setCategory(cat);
        product.setImageUrl(ValidationUtil.optionalUrl(imageUrl));

        if (!productDAO.update(product)) {
            throw new NotFoundException("Product not found");
        }
        return product;
    }

    /**
     * Deletes an owned product after verifying ownership.
     */
    public void deleteProduct(long sellerId, long productId) {
        getOwnedProduct(sellerId, productId);
        if (!productDAO.delete(productId)) {
            throw new NotFoundException("Product not found");
        }
    }
}

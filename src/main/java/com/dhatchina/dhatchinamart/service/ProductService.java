package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.exception.ForbiddenException;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.Product;
import com.dhatchina.dhatchinamart.util.ValidationUtil;

import java.util.List;

public class ProductService {

    private static final int MAX_CATEGORY_LENGTH = 50;

    private final ProductDAO productDAO;

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public List<Product> browse(String keyword, String category) {
        return productDAO.find(keyword, category);
    }

    public List<String> categories() {
        return productDAO.findCategories();
    }

    public Product getById(long id) {
        return productDAO.findById(id)
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
        Product product = getById(productId);
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

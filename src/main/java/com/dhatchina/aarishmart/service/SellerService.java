package com.dhatchina.aarishmart.service;

import com.dhatchina.aarishmart.dao.ProductDAO;
import com.dhatchina.aarishmart.exception.ValidationException;
import com.dhatchina.aarishmart.model.Product;
import com.dhatchina.aarishmart.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.List;

public class SellerService {

    private static final int MAX_CATEGORY_LENGTH = 50;

    private final ProductDAO productDAO;

    public SellerService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public Product createProduct(long sellerId, String name, String description, String price, String stock,
                                 String category, String imageUrl) {
        Product product = new Product();
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

        long id = productDAO.insert(product);
        product.setId(id);
        return product;
    }

    public List<Product> productsForSeller(long sellerId) {
        return productDAO.findBySeller(sellerId);
    }

    public long productCount(long sellerId) {
        return productDAO.countBySeller(sellerId);
    }
}

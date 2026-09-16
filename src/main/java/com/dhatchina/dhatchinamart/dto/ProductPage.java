package com.dhatchina.dhatchinamart.dto;

import com.dhatchina.dhatchinamart.model.Product;

import java.util.List;

/**
 * A single, immutable page of browse results plus the metadata the UI needs
 * to render result counts and pagination.
 */
public class ProductPage {

    private final List<Product> products;
    private final long totalItems;
    private final int page;
    private final int pageSize;
    private final int totalPages;

    public ProductPage(List<Product> products, long totalItems, int page, int pageSize, int totalPages) {
        this.products = products;
        this.totalItems = totalItems;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    public List<Product> getProducts() {
        return products;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < totalPages;
    }
}
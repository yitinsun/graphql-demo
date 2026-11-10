package com.example.graphqldemo.product;

public record ProductFilter(String category, Double minPrice, Double maxPrice, Boolean inStock, String search) {

    public static final ProductFilter NONE = new ProductFilter(null, null, null, null, null);
}

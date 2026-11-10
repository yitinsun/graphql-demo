package com.example.graphqldemo.product;

import org.springframework.data.domain.Sort;

public enum ProductSort {
    NAME_ASC(Sort.by("name").ascending()),
    PRICE_ASC(Sort.by("price").ascending()),
    PRICE_DESC(Sort.by("price").descending()),
    NEWEST(Sort.by("createdAt").descending());

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort.and(Sort.by("id")); // tie-breaker => stable pagination
    }

    public Sort sort() {
        return sort;
    }
}

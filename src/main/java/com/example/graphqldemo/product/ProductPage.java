package com.example.graphqldemo.product;

import java.util.List;

public record ProductPage(List<Product> items, long totalElements, int totalPages, int page) {}

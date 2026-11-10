package com.example.graphqldemo.product;

import com.example.graphqldemo.content.ContentService;
import com.example.graphqldemo.content.ProductContent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProductController {

    private final ProductRepository repository;
    private final ContentService content;

    public ProductController(ProductRepository repository, ContentService content) {
        this.repository = repository;
        this.content = content;
    }

    @QueryMapping
    public ProductPage products(@Argument ProductFilter filter, @Argument ProductSort sort,
                                @Argument int page, @Argument int size) {
        ProductFilter f = filter == null ? ProductFilter.NONE : filter;
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort.sort());
        Page<Product> result = repository.findAll(ProductSpecs.from(f), pageable);
        return new ProductPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    @QueryMapping
    public Product product(@Argument Long id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * PostgreSQL -> Contentful join. Without batching, a page of 20 products would trigger 20 Contentful
     * calls (N+1); here Spring GraphQL collects all products and we make ONE call using sku[in]=...
     * Only runs if the client actually selected `content`.
     */
    @BatchMapping
    public Map<Product, ProductContent> content(List<Product> products) {
        Map<String, ProductContent> bySku = content.productContent(products.stream().map(Product::getSku).toList());
        Map<Product, ProductContent> result = new HashMap<>();
        for (Product p : products) {
            ProductContent c = bySku.get(p.getSku());
            if (c != null) result.put(p, c);
        }
        return result;
    }
}

package com.example.graphqldemo.content;

import com.example.graphqldemo.product.Product;
import com.example.graphqldemo.product.ProductRepository;
import graphql.schema.DataFetchingFieldSelectionSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ContentController {

    private final ContentService content;
    private final ProductRepository products;

    public ContentController(ContentService content, ProductRepository products) {
        this.content = content;
        this.products = products;
    }

    /**
     * Projection push-down: only the Contentful fields the client actually selected in GraphQL are
     * requested from Contentful (via its `select` parameter), keeping payloads small.
     */
    @QueryMapping
    public List<Article> articles(@Argument ArticleFilter filter, @Argument int limit, @Argument int skip,
                                  DataFetchingFieldSelectionSet selection) {
        List<String> select = new ArrayList<>(List.of("sys", "fields.slug"));
        if (selection.contains("title")) select.add("fields.title");
        if (selection.contains("body")) select.add("fields.body");
        if (selection.contains("publishedAt")) select.add("fields.publishedAt");
        if (selection.contains("relatedProducts")) select.add("fields.productSkus");
        return content.articles(filter, select, limit, skip);
    }

    @QueryMapping
    public Article article(@Argument String slug) {
        return content.articleBySlug(slug);
    }

    @QueryMapping
    public EntryPage entries(@Argument String contentType, @Argument List<EntryFilterInput> filters,
                             @Argument List<String> select, @Argument List<String> order,
                             @Argument int limit, @Argument int skip) {
        return content.entries(contentType, filters, select, order, limit, skip);
    }

    /** Contentful -> PostgreSQL join, batched: one SQL query for all articles in the response. */
    @BatchMapping
    public Map<Article, List<Product>> relatedProducts(List<Article> articles) {
        Set<String> skus = articles.stream().flatMap(a -> a.productSkus().stream()).collect(Collectors.toSet());
        Map<String, Product> bySku = products.findBySkuIn(skus).stream()
                .collect(Collectors.toMap(Product::getSku, Function.identity()));

        Map<Article, List<Product>> result = new HashMap<>();
        for (Article a : articles) {
            result.put(a, a.productSkus().stream().map(bySku::get).filter(Objects::nonNull).toList());
        }
        return result;
    }
}

package com.example.graphqldemo.content;

import com.example.graphqldemo.config.ContentfulProperties;
import com.example.graphqldemo.content.EntryQuery.Filter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ContentService {

    private final ContentfulClient client;
    private final ContentfulProperties props;

    public ContentService(ContentfulClient client, ContentfulProperties props) {
        this.client = client;
        this.props = props;
    }

    /** Fully dynamic access, restricted to the configured allow-list of content types. */
    public EntryPage entries(String contentType, List<EntryFilterInput> filters, List<String> select,
                             List<String> order, int limit, int skip) {
        if (!props.allowedContentTypes().contains(contentType)) {
            throw new IllegalArgumentException("Content type not allowed: " + contentType);
        }
        List<Filter> mapped = filters == null ? List.of()
                : filters.stream().map(f -> new Filter(f.field(), f.operator(), f.value())).toList();
        return client.fetch(new EntryQuery(contentType, mapped, select, order, limit, skip, 1));
    }

    public List<Article> articles(ArticleFilter f, List<String> select, int limit, int skip) {
        List<Filter> filters = new ArrayList<>();
        if (f != null) {
            if (notBlank(f.titleContains())) filters.add(new Filter("title", FilterOperator.MATCH, f.titleContains()));
            if (notBlank(f.publishedAfter())) filters.add(new Filter("publishedAt", FilterOperator.GTE, f.publishedAfter()));
            if (notBlank(f.productSku())) filters.add(new Filter("productSkus", FilterOperator.IN, f.productSku()));
        }
        var query = new EntryQuery("article", filters, select, List.of("-fields.publishedAt"), limit, skip, 0);
        return client.fetch(query).items().stream().map(Article::from).toList();
    }

    public Article articleBySlug(String slug) {
        var query = new EntryQuery("article", List.of(new Filter("slug", FilterOperator.EQ, slug)),
                List.of(), List.of(), 1, 0, 0);
        return client.fetch(query).items().stream().findFirst().map(Article::from).orElse(null);
    }

    /** One Contentful call for any number of SKUs (used by the batched Product.content resolver). */
    public Map<String, ProductContent> productContent(Collection<String> skus) {
        if (skus.isEmpty()) return Map.of();
        var query = new EntryQuery("productContent",
                List.of(new Filter("sku", FilterOperator.IN, String.join(",", skus))),
                List.of(), List.of(), skus.size(), 0, 1);
        Map<String, ProductContent> bySku = new HashMap<>();
        client.fetch(query).items().forEach(e -> {
            ProductContent pc = ProductContent.from(e);
            bySku.put(pc.sku(), pc);
        });
        return bySku;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}

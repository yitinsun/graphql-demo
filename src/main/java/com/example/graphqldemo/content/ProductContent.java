package com.example.graphqldemo.content;

import java.util.List;
import java.util.Map;

public record ProductContent(String sku, String title, String description, String imageUrl, List<String> features) {

    public static ProductContent from(Entry e) {
        Map<?, ?> image = e.map("image");
        String url = image == null || image.get("url") == null ? null : image.get("url").toString();
        return new ProductContent(e.str("sku"), e.str("title"), e.str("description"), url, e.strings("features"));
    }
}

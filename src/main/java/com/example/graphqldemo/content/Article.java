package com.example.graphqldemo.content;

import java.util.List;

public record Article(String id, String title, String slug, String body, String publishedAt,
                      List<String> productSkus) {

    public static Article from(Entry e) {
        return new Article(e.id(), e.str("title"), e.str("slug"), e.str("body"), e.str("publishedAt"),
                e.strings("productSkus"));
    }
}

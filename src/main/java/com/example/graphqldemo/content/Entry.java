package com.example.graphqldemo.content;

import java.util.List;
import java.util.Map;

/** A Contentful entry, normalised: sys.id, content type, last update and the (link-resolved) field map. */
public record Entry(String id, String contentType, String updatedAt, Map<String, Object> fields) {

    public String str(String key) {
        Object v = fields.get(key);
        return v == null ? null : v.toString();
    }

    public List<String> strings(String key) {
        return fields.get(key) instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();
    }

    public Map<?, ?> map(String key) {
        return fields.get(key) instanceof Map<?, ?> m ? m : null;
    }
}

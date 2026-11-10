package com.example.graphqldemo.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated Contentful search. Field names and operators are whitelisted (regex / enum) and values are
 * passed as URI variables by the client, so a GraphQL caller cannot inject extra query parameters.
 * Being a record, it is also the cache key.
 */
public record EntryQuery(String contentType, List<Filter> filters, List<String> select, List<String> order,
                         int limit, int skip, int include) {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_.]{1,100}$");

    public record Filter(String field, FilterOperator operator, String value) {
        public Filter {
            field = qualify(field);
            operator = operator == null ? FilterOperator.EQ : operator;
            Objects.requireNonNull(value, "Filter value is required");
        }
    }

    public EntryQuery {
        if (contentType == null || !NAME.matcher(contentType).matches()) {
            throw new IllegalArgumentException("Invalid content type: " + contentType);
        }
        filters = filters == null ? List.of() : List.copyOf(filters);

        List<String> sel = new ArrayList<>();
        if (select != null && !select.isEmpty()) {
            sel.add("sys"); // always keep sys so ids / timestamps can be mapped
            for (String s : select) {
                String q = qualify(s);
                if (!sel.contains(q)) sel.add(q);
            }
        }
        select = List.copyOf(sel);

        List<String> ord = new ArrayList<>();
        if (order != null) {
            for (String o : order) {
                boolean desc = o.startsWith("-");
                String q = qualify(desc ? o.substring(1) : o);
                ord.add(desc ? "-" + q : q);
            }
        }
        order = List.copyOf(ord);

        limit = Math.min(Math.max(limit, 1), 100);
        skip = Math.max(skip, 0);
        include = Math.min(Math.max(include, 0), 2);
    }

    static String qualify(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid field name: " + name);
        }
        if (name.equals("sys") || name.startsWith("sys.") || name.startsWith("fields.")) return name;
        return "fields." + name;
    }

    public List<Map.Entry<String, String>> toParams() {
        List<Map.Entry<String, String>> p = new ArrayList<>();
        p.add(Map.entry("content_type", contentType));
        for (Filter f : filters) p.add(Map.entry(f.field() + f.operator().suffix(), f.value()));
        if (!select.isEmpty()) p.add(Map.entry("select", String.join(",", select)));
        if (!order.isEmpty()) p.add(Map.entry("order", String.join(",", order)));
        p.add(Map.entry("limit", String.valueOf(limit)));
        p.add(Map.entry("skip", String.valueOf(skip)));
        p.add(Map.entry("include", String.valueOf(include)));
        return p;
    }
}

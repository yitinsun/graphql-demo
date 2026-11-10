package com.example.graphqldemo.content;

/** Whitelisted Contentful search operators; the suffix is appended to the field in the query string. */
public enum FilterOperator {
    EQ(""), NE("[ne]"), IN("[in]"), NIN("[nin]"), MATCH("[match]"),
    GT("[gt]"), GTE("[gte]"), LT("[lt]"), LTE("[lte]"), EXISTS("[exists]");

    private final String suffix;

    FilterOperator(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }
}

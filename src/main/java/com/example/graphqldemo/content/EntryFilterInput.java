package com.example.graphqldemo.content;

/** GraphQL input binding for EntryFilter. */
public record EntryFilterInput(String field, FilterOperator operator, String value) {}

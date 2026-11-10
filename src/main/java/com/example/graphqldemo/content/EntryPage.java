package com.example.graphqldemo.content;

import java.util.List;

public record EntryPage(List<Entry> items, int total, int skip, int limit) {}

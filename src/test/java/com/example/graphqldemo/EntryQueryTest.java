package com.example.graphqldemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.graphqldemo.content.EntryQuery;
import com.example.graphqldemo.content.FilterOperator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntryQueryTest {

    @Test
    void buildsQualifiedAndClampedParams() {
        var q = new EntryQuery("article",
                List.of(new EntryQuery.Filter("title", FilterOperator.MATCH, "kafka")),
                List.of("title"), List.of("-sys.updatedAt"), 500, -3, 9);

        assertThat(q.toParams()).contains(
                Map.entry("content_type", "article"),
                Map.entry("fields.title[match]", "kafka"),
                Map.entry("select", "sys,fields.title"),
                Map.entry("order", "-sys.updatedAt"),
                Map.entry("limit", "100"),
                Map.entry("skip", "0"),
                Map.entry("include", "2"));
    }

    @Test
    void rejectsParameterInjectionInFieldNames() {
        assertThatThrownBy(() -> new EntryQuery.Filter("title&access_token", FilterOperator.EQ, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntryQuery("article&x=1", null, null, null, 10, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

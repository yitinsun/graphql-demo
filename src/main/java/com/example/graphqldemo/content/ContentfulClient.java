package com.example.graphqldemo.content;

import com.example.graphqldemo.config.ContentfulProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Thin client for the Contentful Delivery (or Preview) REST API. */
@Component
public class ContentfulClient {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final RestClient rest;
    private final ObjectMapper mapper;

    public ContentfulClient(ContentfulProperties props, RestClient.Builder builder, ObjectMapper mapper) {
        this.mapper = mapper;

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.timeout());
        factory.setReadTimeout(props.timeout());

        String host = props.preview() ? "https://preview.contentful.com" : "https://cdn.contentful.com";
        this.rest = builder
                .requestFactory(factory)
                .baseUrl(host + "/spaces/" + props.spaceId() + "/environments/" + props.environment())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token())
                .build();
    }

    /** Cached for 60s (see application.yml) keyed by the whole query, so repeated GraphQL calls stay cheap. */
    @Cacheable(cacheNames = "contentful", key = "#query")
    public EntryPage fetch(EntryQuery query) {
        List<Map.Entry<String, String>> params = query.toParams();
        Map<String, String> vars = new HashMap<>();

        JsonNode root;
        try {
            root = rest.get().uri(b -> {
                b.path("/entries");
                for (int i = 0; i < params.size(); i++) {
                    // values travel as URI variables => fully encoded, never parsed as template/query syntax
                    b.queryParam(params.get(i).getKey(), "{v" + i + "}");
                    vars.put("v" + i, params.get(i).getValue());
                }
                return b.build(vars);
            }).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new ContentfulException("Contentful responded with HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new ContentfulException("Contentful is unreachable", e);
        }
        return parse(root);
    }

    private EntryPage parse(JsonNode root) {
        // index linked entries/assets from "includes" so links can be resolved without extra calls
        Map<String, JsonNode> index = new HashMap<>();
        for (String type : List.of("Entry", "Asset")) {
            for (JsonNode n : root.path("includes").path(type)) {
                index.put(type + ":" + n.path("sys").path("id").asText(), n);
            }
        }

        List<Entry> items = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            Map<String, Object> fields = new LinkedHashMap<>();
            if (item.has("fields")) {
                mapper.convertValue(item.get("fields"), MAP).forEach((k, v) -> fields.put(k, resolve(v, index)));
            }
            JsonNode sys = item.path("sys");
            items.add(new Entry(
                    sys.path("id").asText(),
                    sys.path("contentType").path("sys").path("id").asText(),
                    sys.path("updatedAt").asText(null),
                    fields));
        }
        return new EntryPage(items, root.path("total").asInt(), root.path("skip").asInt(), root.path("limit").asInt());
    }

    private Object resolve(Object value, Map<String, JsonNode> index) {
        if (value instanceof Map<?, ?> m && m.get("sys") instanceof Map<?, ?> sys && "Link".equals(sys.get("type"))) {
            JsonNode target = index.get(sys.get("linkType") + ":" + sys.get("id"));
            if (target == null) return null; // unpublished / not included
            if ("Asset".equals(sys.get("linkType"))) {
                JsonNode file = target.path("fields").path("file");
                Map<String, Object> asset = new LinkedHashMap<>();
                asset.put("title", target.path("fields").path("title").asText(null));
                asset.put("url", file.has("url") ? "https:" + file.get("url").asText() : null);
                asset.put("contentType", file.path("contentType").asText(null));
                return asset;
            }
            return mapper.convertValue(target.path("fields"), MAP); // linked entry: one level deep
        }
        if (value instanceof List<?> list) {
            return list.stream().map(v -> resolve(v, index)).toList();
        }
        return value;
    }
}

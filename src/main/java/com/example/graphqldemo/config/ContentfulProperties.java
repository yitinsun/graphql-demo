package com.example.graphqldemo.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("contentful")
public record ContentfulProperties(
        String spaceId,
        String environment,
        String token,
        boolean preview,
        Duration timeout,
        List<String> allowedContentTypes) {

    public ContentfulProperties {
        if (environment == null || environment.isBlank()) environment = "master";
        if (timeout == null) timeout = Duration.ofSeconds(5);
        if (allowedContentTypes == null) allowedContentTypes = List.of();
    }
}

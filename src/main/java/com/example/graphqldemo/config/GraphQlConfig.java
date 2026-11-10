package com.example.graphqldemo.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
@EnableCaching
public class GraphQlConfig {

    /** Registers the JSON scalar used by Entry.fields. */
    @Bean
    RuntimeWiringConfigurer scalars() {
        return wiring -> wiring.scalar(ExtendedScalars.Json);
    }
}

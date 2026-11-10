package com.example.graphqldemo.config;

import com.example.graphqldemo.content.ContentfulException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Turns failures into proper GraphQL errors. Because the affected fields are nullable, the rest of the
 * response is still returned (e.g. products still load when Contentful is down).
 */
@ControllerAdvice
public class GraphQlExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError onContentful(ContentfulException ex, DataFetchingEnvironment env) {
        return error(ErrorType.INTERNAL_ERROR, "Content service error: " + ex.getMessage(), env);
    }

    @GraphQlExceptionHandler
    public GraphQLError onBadInput(IllegalArgumentException ex, DataFetchingEnvironment env) {
        return error(ErrorType.BAD_REQUEST, ex.getMessage(), env);
    }

    private static GraphQLError error(ErrorType type, String message, DataFetchingEnvironment env) {
        return GraphQLError.newError()
                .errorType(type)
                .message(message)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}

package com.example.graphqldemo.content;

public class ContentfulException extends RuntimeException {
    public ContentfulException(String message, Throwable cause) {
        super(message, cause);
    }
}

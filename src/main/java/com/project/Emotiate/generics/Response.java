package com.project.Emotiate.generics;

public record Response<T>(T data, String message, int status) {

    // Convenience constructor when only status is provided
    public Response(int status) {
        this(null, null, status);
    }

    // Convenience constructor when data and status are provided
    public Response(T data, int status) {
        this(data, null, status);
    }

    // Convenience constructor when message and status are provided
    public Response(String message, int status) {
        this(null, message, status);
    }
}

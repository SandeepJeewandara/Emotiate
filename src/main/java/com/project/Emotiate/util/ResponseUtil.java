package com.project.Emotiate.util;

import com.project.Emotiate.generics.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseUtil {

    //  Convenience constructor for success responses
    public static <T> ResponseEntity<Response<T>> success(T data, String message) {
        return ResponseEntity.ok(
                new Response<>(data, message, HttpStatus.OK.value())
        );
    }

    // Convenience constructor for created responses
    public static <T> ResponseEntity<Response<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Response<>(data, message, HttpStatus.CREATED.value())
        );
    }
}

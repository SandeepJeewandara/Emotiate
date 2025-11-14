package com.project.Emotiate.util;

import com.project.Emotiate.generics.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResponseUtil {

    //  Convenience constructor for success responses
    public static <T> ResponseEntity<Response<T>> success(T data, String message) {

        log.info(message);
        return ResponseEntity.ok(
                new Response<>(data, message, HttpStatus.OK.value())
        );
    }

    // Convenience constructor for created responses
    public static <T> ResponseEntity<Response<T>> created(T data, String message) {

        log.info(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Response<>(data, message, HttpStatus.CREATED.value())
        );
    }

    // Convenience constructor for noContent responses
    public static <T> ResponseEntity<Response<T>> noContent() {

        log.info("No content");
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}

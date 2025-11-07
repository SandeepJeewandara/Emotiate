package com.project.Emotiate.exception;

import com.project.Emotiate.generics.Response;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle uncaught general exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Object>> handleGeneralException(Exception ex) {
        Response<Object> response = new Response<>(
                null,
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    // Handle Custom Exception
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Response<Object>> handleBusinessException(CustomException ex) {
        Response<Object> response = new Response<>(
                null,
                ex.getMessage(),
                ex.getStatusCode()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }


    // Handle JADE StaleProxyException
    @ExceptionHandler(StaleProxyException.class)
    public ResponseEntity<Response<Object>> handleStaleProxyException(StaleProxyException ex) {
        Response<Object> response = new Response<>(
                null,
                "Agent container error: " + ex.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }


    // Handle ControllerException (custom)
    @ExceptionHandler(ControllerException.class)
    public ResponseEntity<Response<Object>> handleControllerException(ControllerException ex) {
        Response<Object> response = new Response<>(
                null,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}

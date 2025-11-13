package com.project.Emotiate.exception;

import com.project.Emotiate.generics.Response;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.NoSuchElementException;

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


    // Handle WebClientResponseException for external API errors
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Response<Object>> handleWebClientException(WebClientResponseException ex) {
        Response<Object> response = new Response<>(
                null,
                "External API error: " + ex.getResponseBodyAsString(),
                ex.getStatusCode().value()
        );

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }



    // Handle Authentication Errors
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Response<Object>> handleBadCredentials(BadCredentialsException ex) {
        Response<Object> response = new Response<>(
                null,
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


    // Handle Internal Authentication Errors
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<Response<Object>> handleInternalAuthentication(InternalAuthenticationServiceException ex) {
        Response<Object> response = new Response<>(
                null,
                "Invalid username or password"
                , HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


    // Handle NoSuch Element Errors
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Response<Object>> handleNoSuchElement(NoSuchElementException ex) {
        Response<Object> response = new Response<>(
                null,
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    // Handle Authorization Errors (Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Response<Object> response = new Response<>(
                null,
                "Access denied. Insufficient permissions",
                HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }


    // Handle Endpoint Not Found Exception
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Response<Object>> handleNoHandlerFound(NoHandlerFoundException ex) {
        Response<Object> response = new Response<>(
                null,
                "Endpoint not found: " + ex.getRequestURL(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    // Handle Null Pointer Exception
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Response<Object>> handleNullPointerException(NullPointerException ex) {
        Response<Object> response = new Response<>(
                null,
                "A null value was encountered unexpectedly",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    // Handle NoResource Found Exception
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response<Object>> NoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        Response<Object> response = new Response<>(
                null,
                "Endpoint not found: " + ex.getResourcePath(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    // Handle User Disabled Exception
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Response<Object>> handleDisabledException(DisabledException ex) {
        Response<Object> response = new Response<>(
                null,
                "User is disabled",
                HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


    // Handle HttpMediaTypeNotSupportedException Exception
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Response<Object>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        Response<Object> response = new Response<>(
                null,
                "Unsupported media type. Please use one of the supported types",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()).body(response);
    }
}

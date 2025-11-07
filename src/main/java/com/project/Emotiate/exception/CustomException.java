package com.project.Emotiate.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final int statusCode;

    // Constructor for Custom Exception
    public CustomException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}

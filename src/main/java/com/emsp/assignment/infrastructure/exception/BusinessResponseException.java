package com.emsp.assignment.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class BusinessResponseException extends RuntimeException {
    private final HttpStatus status;

    public BusinessResponseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
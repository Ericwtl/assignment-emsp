package com.emsp.assignment.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class IllegalCardOperationException extends RuntimeException {
    public IllegalCardOperationException(String message) {
        super(message);
    }
}
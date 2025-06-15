package com.emsp.assignment.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class IllegalAccountOperationException extends RuntimeException {
    public IllegalAccountOperationException(String message) {
        super(message);
    }
}
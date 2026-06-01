package com.document.domain.exception;

/**
 * Thrown when business rules are violated
 * (e.g. invalid sequence, invalid state transition)
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
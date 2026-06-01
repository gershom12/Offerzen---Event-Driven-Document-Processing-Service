package com.document.domain.exception;

/**
 * Marks transient failures that should be retried:
 * - DB timeouts
 * - Kafka transient errors
 * - network failures
 */
public class RetryableException extends RuntimeException {

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
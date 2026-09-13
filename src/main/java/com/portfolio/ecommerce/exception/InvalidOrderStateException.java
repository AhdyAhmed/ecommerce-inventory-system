package com.portfolio.ecommerce.exception;

/**
 * Thrown when an operation is attempted against an order in a status that
 * doesn't allow it (e.g. confirming an already-cancelled order). Mapped to
 * 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }

}

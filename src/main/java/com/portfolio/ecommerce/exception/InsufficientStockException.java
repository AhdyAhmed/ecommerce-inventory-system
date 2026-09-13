package com.portfolio.ecommerce.exception;

/**
 * A business rule violation, not a client input error - the request is
 * well-formed, it just can't be satisfied right now. Mapped to 409 Conflict
 * by {@link GlobalExceptionHandler}, distinct from the 400s used for bad
 * input and the 404s used for missing resources.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

}

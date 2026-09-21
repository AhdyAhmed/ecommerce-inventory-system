package com.portfolio.ecommerce.exception;

/**
 * Thrown when an order references a product that exists but has been
 * discontinued ({@code active == false}). Same category of problem as
 * {@link InsufficientStockException}: the request is well-formed and the
 * product is real, it just can't be ordered right now. Mapped to 409
 * Conflict by {@link GlobalExceptionHandler}, alongside the other two
 * order-time business-rule exceptions.
 */
public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(String message) {
        super(message);
    }

}

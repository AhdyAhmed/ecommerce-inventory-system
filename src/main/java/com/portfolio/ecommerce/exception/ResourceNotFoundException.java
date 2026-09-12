package com.portfolio.ecommerce.exception;

/**
 * Thrown when a requested entity (or a referenced related entity, e.g. a
 * category ID on a product request) doesn't exist. Translated into a 404 by
 * {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}

package com.portfolio.ecommerce.exception;

/**
 * Thrown when a requested entity (or a referenced related entity, e.g. a
 * category ID on a product request) doesn't exist.
 *
 * NOTE: there's no @ControllerAdvice wired up yet to translate this into a
 * proper 404 response - that lands on Day 5. Until then, this surfaces to
 * clients as a generic 500. Introducing the exception now (rather than
 * bolting it on later) keeps the service layer's contract honest from the
 * start.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}

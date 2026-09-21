package com.portfolio.ecommerce.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single place where every exception gets translated into a consistent
 * {@link ErrorResponse}. Handlers are ordered from most specific to most
 * generic - Spring picks the closest match, but the fallback at the bottom
 * guarantees a client never sees a raw stack trace or Spring's default
 * whitelabel error page.
 *
 * Every handler logs at a level matching who's actually at fault: 4xx
 * responses (bad input, a missing resource, a business-rule conflict) are
 * expected, client-caused outcomes and log at {@code warn} with just enough
 * context to correlate a log line with a support ticket - not the full
 * stack trace, since these aren't bugs. Only the unexpected-exception
 * fallback logs at {@code error} with the full stack trace, because that's
 * the one case where something actually went wrong on this side.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("404 on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse body = baseResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * All three business-rule conflicts share this handler: the request is
     * well-formed and every referenced ID is real, it just can't be
     * satisfied right now (not enough stock, the product was discontinued,
     * or the order isn't in a state that allows this transition).
     */
    @ExceptionHandler({InsufficientStockException.class, InvalidOrderStateException.class, ProductNotAvailableException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
        log.warn("409 on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse body = baseResponse(HttpStatus.CONFLICT, ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Fires when Hibernate's {@code @Version} check on {@code Product}
     * detects that two requests read the same row and both tried to commit a
     * change - typically two orders decrementing the same product's stock at
     * the same time. Turning this into a clean 409 instead of a raw 500 is
     * the stopgap Day 12 commits to; it stops a client from seeing a stack
     * trace, but it does nothing to make the *losing* request succeed. A
     * production-grade fix (retry-with-backoff, or a pessimistic lock on the
     * product row for the duration of the stock check) is explicitly
     * deferred to Project 3.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("409 (optimistic lock conflict) on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse body = baseResponse(HttpStatus.CONFLICT,
                "This resource was modified concurrently by another request. Please retry.", request).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.merge(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        (existing, incoming) -> existing + "; " + incoming
                )
        );

        log.warn("400 (validation) on {} {}: {}", request.getMethod(), request.getRequestURI(), fieldErrors);
        ErrorResponse body = baseResponse(HttpStatus.BAD_REQUEST, "Validation failed", request)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("400 (malformed JSON) on {} {}", request.getMethod(), request.getRequestURI());
        ErrorResponse body = baseResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        ErrorResponse body = baseResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ErrorResponse.ErrorResponseBuilder baseResponse(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI());
    }

}

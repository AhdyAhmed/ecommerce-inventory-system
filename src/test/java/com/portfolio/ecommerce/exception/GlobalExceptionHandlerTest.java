package com.portfolio.ecommerce.exception;

import com.portfolio.ecommerce.domain.Product;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Deterministic unit tests for every exception-to-HTTP-status mapping this
 * class is responsible for, including the two Day 12 additions
 * ({@link ProductNotAvailableException} and
 * {@link ObjectOptimisticLockingFailureException}). No Spring context, no
 * MockMvc - the handler methods are plain methods, so calling them directly
 * with a mocked {@link HttpServletRequest} is enough, and it's deterministic
 * in a way that actually reproducing a concurrent-write conflict for the
 * optimistic-lock case would not be.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/products/999");
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404")
    void handlesNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Product not found with id: 999"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found with id: 999");
        assertThat(response.getBody().getPath()).isEqualTo("/api/products/999");
        assertThat(response.getBody().getFieldErrors()).isNull();
    }

    @Test
    @DisplayName("InsufficientStockException -> 409")
    void handlesInsufficientStock() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new InsufficientStockException("Insufficient stock for product 'Laptop'"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("Insufficient stock");
    }

    @Test
    @DisplayName("InvalidOrderStateException -> 409")
    void handlesInvalidOrderState() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new InvalidOrderStateException("Order 5 is already cancelled"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Day 12: ProductNotAvailableException -> 409")
    void handlesProductNotAvailable() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new ProductNotAvailableException("Product 'Laptop' has been discontinued and can't be ordered"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("discontinued");
    }

    @Test
    @DisplayName("Day 12: ObjectOptimisticLockingFailureException -> 409 with a generic retry message, not the raw Hibernate message")
    void handlesOptimisticLockConflict() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException(Product.class, 42L);

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage())
                .contains("modified concurrently")
                .doesNotContain("ObjectOptimisticLockingFailureException"); // client shouldn't see the exception class name
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 with one fieldErrors entry per violation")
    void handlesValidationFailure() {
        FieldError nameError = new FieldError("productRequestDto", "name", "Name is required");
        FieldError priceError = new FieldError("productRequestDto", "price", "Price must be greater than zero");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(nameError, priceError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getFieldErrors())
                .containsEntry("name", "Name is required")
                .containsEntry("price", "Price must be greater than zero");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 with a fixed, safe message (not the raw parser error)")
    void handlesMalformedJson() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON request body");
    }

    @Test
    @DisplayName("any other Exception -> 500 with a generic message, never the exception's own message")
    void handlesUnexpectedException() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("some internal detail that shouldn't leak to clients"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

}

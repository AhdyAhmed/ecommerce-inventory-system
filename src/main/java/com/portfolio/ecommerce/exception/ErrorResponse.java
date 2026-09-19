package com.portfolio.ecommerce.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform shape for every error response the API returns, whether it's a
 * validation failure, a missing resource, or an unexpected server error.
 * `fieldErrors` is only populated for validation failures (JsonInclude.NON_NULL
 * keeps it out of the JSON otherwise).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response shape returned by every non-2xx response.")
public class ErrorResponse {

    @Schema(description = "When the error occurred.", example = "2026-09-19T14:32:07.123Z")
    private final Instant timestamp;

    @Schema(description = "HTTP status code.", example = "404")
    private final int status;

    @Schema(description = "HTTP status reason phrase.", example = "Not Found")
    private final String error;

    @Schema(description = "Human-readable summary of what went wrong.", example = "Product not found with id: 999")
    private final String message;

    @Schema(description = "The request path that produced this error.", example = "/api/products/999")
    private final String path;

    @Schema(description = "Present only on 400 validation failures - one entry per invalid field, keyed by field name.",
            example = "{\"name\": \"Name is required\", \"price\": \"Price must be greater than zero\"}")
    private final Map<String, String> fieldErrors;

}

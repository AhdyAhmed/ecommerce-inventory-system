package com.portfolio.ecommerce.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Map<String, String> fieldErrors;

}

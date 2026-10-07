package com.fantasyzone.exception;

import com.fantasyzone.controller.AssistantController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns LlmException into the project's standard error JSON. It only applies to AssistantController and runs before
 * GlobalExceptionHandler, whose catch-all would otherwise turn these into HTTP 500 errors. Nothing existing is edited.
 */
@RestControllerAdvice(assignableTypes = AssistantController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LlmExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(LlmExceptionHandler.class);

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ErrorResponse> handle(LlmException e) {
        if (e.getStatus().is5xxServerError()) log.warn("AI assistant problem ({}): {}", e.getStatus().value(), e.getMessage());
        return ResponseEntity.status(e.getStatus())
            .body(ErrorResponse.of(e.getStatus().value(), e.getStatus().getReasonPhrase(), e.getMessage()));
    }
}

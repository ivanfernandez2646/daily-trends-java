package com.ivanfernandez.dailytrends.apps.cmsbackend.errors;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.ConflictError;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.NotFoundError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidArgumentError.class)
    ResponseEntity<ErrorResponse> invalidArgument(InvalidArgumentError error) {
        return body(HttpStatus.BAD_REQUEST, messageOf(error));
    }

    @ExceptionHandler(NotFoundError.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundError error) {
        return body(HttpStatus.NOT_FOUND, messageOf(error));
    }

    @ExceptionHandler(ConflictError.class)
    ResponseEntity<ErrorResponse> conflict(ConflictError error) {
        return body(HttpStatus.CONFLICT, messageOf(error));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        // Framework errors (unknown route, method not allowed, ...) carry their own status and headers.
        if (exception instanceof org.springframework.web.ErrorResponse frameworkError) {
            HttpStatusCode status = frameworkError.getStatusCode();
            HttpStatus known = HttpStatus.resolve(status.value());
            String description = known != null ? known.getReasonPhrase() : "Error";
            return ResponseEntity.status(status)
                    .headers(frameworkError.getHeaders())
                    .body(new ErrorResponse(description));
        }
        LOGGER.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private static String messageOf(RuntimeException error) {
        return Objects.requireNonNullElse(error.getMessage(), "");
    }

    private static ResponseEntity<ErrorResponse> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}

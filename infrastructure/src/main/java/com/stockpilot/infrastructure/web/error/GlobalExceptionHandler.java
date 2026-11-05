package com.stockpilot.infrastructure.web.error;

import com.stockpilot.core.exception.AuthenticationFailedException;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.exception.RequestInProgressException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.infrastructure.web.filter.RequestIdFilter;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Translates every failure into an RFC 7807 problem detail. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "urn:stockpilot:problem:";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "resource-not-found", exception.getMessage(), exception, request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Object> handleBusinessRule(BusinessRuleException exception, WebRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "business-rule-violation", exception.getMessage(),
                exception, request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Object> handleInvalidRequest(InvalidRequestException exception, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", exception.getMessage(), exception, request);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Object> handleAuthenticationFailed(AuthenticationFailedException exception,
                                                            WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", exception.getMessage(), exception, request);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<Object> handleConcurrentUpdate(Exception exception,
                                                        WebRequest request) {
        return problem(HttpStatus.CONFLICT, "concurrent-update",
                "The resource was modified by another request, please retry", exception, request);
    }

    @ExceptionHandler(RequestInProgressException.class)
    public ResponseEntity<Object> handleRequestInProgress(RequestInProgressException exception, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "request-in-progress", exception.getMessage(), exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Unexpected error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "An unexpected error occurred",
                exception, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        body.setTitle("Validation Failed");
        body.setType(URI.create(TYPE_PREFIX + "validation-failed"));
        List<FieldViolation> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(java.util.Comparator.comparing(FieldViolation::field))
                .toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail detail) {
            enrich(detail);
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String slug, String detail, Exception exception,
                                           WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create(TYPE_PREFIX + slug));
        return handleExceptionInternal(exception, body, new HttpHeaders(), status, request);
    }

    private void enrich(ProblemDetail detail) {
        if (detail.getProperties() == null || !detail.getProperties().containsKey("timestamp")) {
            detail.setProperty("timestamp", Instant.now());
        }
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            detail.setProperty("requestId", requestId);
        }
    }

    public record FieldViolation(String field, String message) {
    }
}

package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class IdempotencyHeaders {

    static final String KEY = "Idempotency-Key";
    static final String REPLAYED = "Idempotent-Replayed";

    private IdempotencyHeaders() {
    }

    static <T> ResponseEntity<T> created(Result<T> result) {
        return respond(HttpStatus.CREATED, result);
    }

    static <T> ResponseEntity<T> ok(Result<T> result) {
        return respond(HttpStatus.OK, result);
    }

    private static <T> ResponseEntity<T> respond(HttpStatus status, Result<T> result) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (result.replayed()) {
            builder.header(REPLAYED, "true");
        }
        return builder.body(result.body());
    }
}

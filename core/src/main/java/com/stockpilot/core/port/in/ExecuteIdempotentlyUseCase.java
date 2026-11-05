package com.stockpilot.core.port.in;

import java.util.function.Supplier;

public interface ExecuteIdempotentlyUseCase {

    /**
     * Runs the action once per (scope, key). A retry with the same payload gets the stored response back
     * and the action is not run again.
     */
    <T> Result<T> execute(String scope, String key, Object request, Class<T> responseType, Supplier<T> action);

    record Result<T>(T body, boolean replayed) {
    }
}

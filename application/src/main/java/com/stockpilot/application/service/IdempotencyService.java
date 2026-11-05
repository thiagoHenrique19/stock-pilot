package com.stockpilot.application.service;

import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.exception.RequestInProgressException;
import com.stockpilot.core.idempotency.RequestFingerprint;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.out.IdempotencyStore;
import com.stockpilot.core.port.out.PayloadCodec;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IdempotencyService implements ExecuteIdempotentlyUseCase {

    static final int MAX_KEY_LENGTH = 255;

    private final IdempotencyStore store;
    private final PayloadCodec codec;
    private final TransactionTemplate transaction;
    private final Duration staleAfter;

    public IdempotencyService(IdempotencyStore store, PayloadCodec codec, TransactionTemplate transaction,
                              @Value("${app.idempotency.stale-after:2m}") Duration staleAfter) {
        this.staleAfter = staleAfter;
        this.store = store;
        this.codec = codec;
        this.transaction = transaction;
    }

    @Override
    public <T> Result<T> execute(String scope, String key, Object request, Class<T> responseType,
                                 Supplier<T> action) {
        requireValid(key);
        String hash = RequestFingerprint.of(codec.write(request));
        if (store.reserve(scope, key, hash)) {
            return new Result<>(run(scope, key, action), false);
        }
        var existing = store.find(scope, key).orElseThrow(() -> new RequestInProgressException("Retry the request"));
        if (!existing.requestHash().equals(hash)) {
            throw new BusinessRuleException("Idempotency-Key was already used with a different request");
        }
        if (existing.isCompleted()) {
            return new Result<>(codec.read(existing.responseSnapshot(), responseType), true);
        }
        if (store.takeOverStale(scope, key, hash, Instant.now().minus(staleAfter))) {
            return new Result<>(run(scope, key, action), false);
        }
        throw new RequestInProgressException("A request with this Idempotency-Key is still being processed");
    }

    private <T> T run(String scope, String key, Supplier<T> action) {
        try {
            return transaction.execute(status -> {
                T response = action.get();
                store.complete(scope, key, codec.write(response));
                return response;
            });
        } catch (RuntimeException failure) {
            store.release(scope, key);
            throw failure;
        }
    }

    private static void requireValid(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH || !key.chars().allMatch(c -> c > 32 && c < 127)) {
            throw new InvalidRequestException("Idempotency-Key must be 1 to " + MAX_KEY_LENGTH
                    + " printable ASCII characters without spaces");
        }
    }
}

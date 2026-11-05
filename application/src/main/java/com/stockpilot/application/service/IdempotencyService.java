package com.stockpilot.application.service;

import com.stockpilot.core.exception.RequestInProgressException;
import com.stockpilot.core.idempotency.RequestFingerprint;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.out.IdempotencyStore;
import com.stockpilot.core.port.out.PayloadCodec;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IdempotencyService implements ExecuteIdempotentlyUseCase {

    private final IdempotencyStore store;
    private final PayloadCodec codec;
    private final TransactionTemplate transaction;

    public IdempotencyService(IdempotencyStore store, PayloadCodec codec, TransactionTemplate transaction) {
        this.store = store;
        this.codec = codec;
        this.transaction = transaction;
    }

    @Override
    public <T> Result<T> execute(String scope, String key, Object request, Class<T> responseType,
                                 Supplier<T> action) {
        String hash = RequestFingerprint.of(codec.write(request));
        if (store.reserve(scope, key, hash)) {
            return new Result<>(run(scope, key, action), false);
        }
        var existing = store.find(scope, key).orElseThrow(() -> new RequestInProgressException("Retry the request"));
        if (existing.isCompleted()) {
            return new Result<>(codec.read(existing.responseSnapshot(), responseType), true);
        }
        throw new RequestInProgressException("A request with this Idempotency-Key is still being processed");
    }

    private <T> T run(String scope, String key, Supplier<T> action) {
        return transaction.execute(status -> {
            T response = action.get();
            store.complete(scope, key, codec.write(response));
            return response;
        });
    }
}

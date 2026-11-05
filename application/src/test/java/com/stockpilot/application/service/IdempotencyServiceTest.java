package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.core.idempotency.IdempotencyRecord;
import com.stockpilot.core.idempotency.RequestFingerprint;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase.Result;
import com.stockpilot.core.port.out.IdempotencyStore;
import com.stockpilot.core.port.out.PayloadCodec;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private IdempotencyStore store;
    @Mock private PayloadCodec codec;
    @Mock private PlatformTransactionManager transactionManager;

    private IdempotencyService service;
    private final AtomicInteger runs = new AtomicInteger();

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(store, codec, new TransactionTemplate(transactionManager));
        org.mockito.Mockito.lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
        org.mockito.Mockito.lenient().when(codec.write("request")).thenReturn("{\"request\":true}");
        org.mockito.Mockito.lenient().when(codec.write("response")).thenReturn("{\"response\":true}");
    }

    private String hash() {
        return RequestFingerprint.of("{\"request\":true}");
    }

    private String run() {
        runs.incrementAndGet();
        return "response";
    }

    @Test
    void shouldRunTheActionAndStoreTheSnapshotOnTheFirstCall() {
        when(store.reserve("sales", "k1", hash())).thenReturn(true);

        Result<String> result = service.execute("sales", "k1", "request", String.class, this::run);

        assertThat(result.body()).isEqualTo("response");
        assertThat(result.replayed()).isFalse();
        assertThat(runs).hasValue(1);
        verify(store).complete("sales", "k1", "{\"response\":true}");
    }

    @Test
    void shouldReplayTheStoredResponseWithoutRunningTheActionAgain() {
        when(store.reserve("sales", "k1", hash())).thenReturn(false);
        when(store.find("sales", "k1")).thenReturn(Optional.of(new IdempotencyRecord("sales", "k1", hash(),
                IdempotencyRecord.Status.COMPLETED, "{\"response\":true}", Instant.now())));
        when(codec.read("{\"response\":true}", String.class)).thenReturn("response");

        Result<String> result = service.execute("sales", "k1", "request", String.class, this::run);

        assertThat(result.body()).isEqualTo("response");
        assertThat(result.replayed()).isTrue();
        assertThat(runs).hasValue(0);
        verify(store, never()).complete(any(), any(), any());
    }
}

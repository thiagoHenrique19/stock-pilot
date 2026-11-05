package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.exception.RequestInProgressException;
import com.stockpilot.core.idempotency.IdempotencyRecord;
import com.stockpilot.core.idempotency.RequestFingerprint;
import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase.Result;
import com.stockpilot.core.port.out.IdempotencyStore;
import com.stockpilot.core.port.out.PayloadCodec;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
        service = new IdempotencyService(store, codec, new TransactionTemplate(transactionManager),
                Duration.ofMinutes(2));
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

    @Test
    void shouldRejectTheSameKeyWithADifferentPayload() {
        when(store.reserve("sales", "k1", hash())).thenReturn(false);
        when(store.find("sales", "k1")).thenReturn(Optional.of(new IdempotencyRecord("sales", "k1",
                RequestFingerprint.of("something else"), IdempotencyRecord.Status.COMPLETED, "{}", Instant.now())));

        assertThatThrownBy(() -> service.execute("sales", "k1", "request", String.class, this::run))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
        assertThat(runs).hasValue(0);
    }

    @Test
    void shouldRejectAMismatchedPayloadEvenWhileTheFirstRequestIsStillRunning() {
        when(store.reserve("sales", "k1", hash())).thenReturn(false);
        when(store.find("sales", "k1")).thenReturn(Optional.of(new IdempotencyRecord("sales", "k1",
                RequestFingerprint.of("something else"), IdempotencyRecord.Status.IN_PROGRESS, null, Instant.now())));

        assertThatThrownBy(() -> service.execute("sales", "k1", "request", String.class, this::run))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldAskTheClientToRetryWhileTheFirstRequestIsStillRunning() {
        when(store.reserve("sales", "k1", hash())).thenReturn(false);
        when(store.find("sales", "k1")).thenReturn(Optional.of(new IdempotencyRecord("sales", "k1", hash(),
                IdempotencyRecord.Status.IN_PROGRESS, null, Instant.now())));

        assertThatThrownBy(() -> service.execute("sales", "k1", "request", String.class, this::run))
                .isInstanceOf(RequestInProgressException.class);
        assertThat(runs).hasValue(0);
    }

    @Test
    void shouldAskTheClientToRetryWhenTheReservationVanishedBetweenTheTwoLookups() {
        when(store.reserve("sales", "k1", hash())).thenReturn(false);
        when(store.find("sales", "k1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute("sales", "k1", "request", String.class, this::run))
                .isInstanceOf(RequestInProgressException.class);
    }

    @Test
    void shouldTakeOverAReservationLeftBehindByACrashedRequest() {
        Instant started = Instant.now().minusSeconds(600);
        when(store.reserve("sales", "k1", hash())).thenReturn(false);
        when(store.find("sales", "k1")).thenReturn(Optional.of(new IdempotencyRecord("sales", "k1", hash(),
                IdempotencyRecord.Status.IN_PROGRESS, null, started)));
        when(store.takeOverStale(eq("sales"), eq("k1"), eq(hash()), any(Instant.class))).thenReturn(true);

        Result<String> result = service.execute("sales", "k1", "request", String.class, this::run);

        assertThat(result.replayed()).isFalse();
        assertThat(runs).hasValue(1);
        verify(store).complete("sales", "k1", "{\"response\":true}");
    }

    @Test
    void shouldReleaseTheKeyWhenTheActionFailsSoTheClientCanRetry() {
        when(store.reserve("sales", "k1", hash())).thenReturn(true);

        assertThatThrownBy(() -> service.execute("sales", "k1", "request", String.class, () -> {
            throw new BusinessRuleException("Insufficient stock");
        })).isInstanceOf(BusinessRuleException.class);

        verify(store).release("sales", "k1");
        verify(store, never()).complete(any(), any(), any());
    }

    @Test
    void shouldReleaseTheKeyWhenStoringTheSnapshotFails() {
        when(store.reserve("sales", "k1", hash())).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("db down")).when(store).complete(any(), any(), any());

        assertThatThrownBy(() -> service.execute("sales", "k1", "request", String.class, this::run))
                .isInstanceOf(IllegalStateException.class);

        verify(store).release("sales", "k1");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "has space", "tab\tkey", "accént", "line\nbreak"})
    void shouldRejectMalformedKeys(String key) {
        assertThatThrownBy(() -> service.execute("sales", key, "request", String.class, this::run))
                .isInstanceOf(InvalidRequestException.class);
        verify(store, never()).reserve(any(), any(), any());
    }

    @Test
    void shouldRejectKeysLongerThan255Characters() {
        assertThatThrownBy(() -> service.execute("sales", "k".repeat(256), "request", String.class, this::run))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldAcceptKeysOfExactly255Characters() {
        String key = "k".repeat(255);
        when(store.reserve("sales", key, hash())).thenReturn(true);

        assertThat(service.execute("sales", key, "request", String.class, this::run).replayed()).isFalse();
    }
}

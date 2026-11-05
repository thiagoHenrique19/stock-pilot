package com.stockpilot.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockpilot.core.domain.PaymentMethod;
import com.stockpilot.core.domain.SaleStatus;
import com.stockpilot.core.port.in.dto.SaleItemRequest;
import com.stockpilot.core.port.in.dto.SaleRequest;
import com.stockpilot.core.port.in.dto.SaleResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class JacksonPayloadCodecTest {

    private final JacksonPayloadCodec codec = new JacksonPayloadCodec(new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void shouldWriteTheSameTextForEqualRequests() {
        SaleRequest a = new SaleRequest(PaymentMethod.PIX, null, null, null, List.of(new SaleItemRequest(1L, 2)));
        SaleRequest b = new SaleRequest(PaymentMethod.PIX, null, null, null, List.of(new SaleItemRequest(1L, 2)));

        assertThat(codec.write(a)).isEqualTo(codec.write(b));
    }

    @Test
    void shouldWriteDifferentTextWhenAQuantityChanges() {
        SaleRequest a = new SaleRequest(PaymentMethod.PIX, null, null, null, List.of(new SaleItemRequest(1L, 2)));
        SaleRequest b = new SaleRequest(PaymentMethod.PIX, null, null, null, List.of(new SaleItemRequest(1L, 3)));

        assertThat(codec.write(a)).isNotEqualTo(codec.write(b));
    }

    @Test
    void shouldRoundTripAResponseIncludingMoneyAndInstants() {
        SaleResponse sale = new SaleResponse(7L, "SALE-000007", SaleStatus.COMPLETED, PaymentMethod.CASH, List.of(),
                new BigDecimal("40.00"), BigDecimal.ZERO, new BigDecimal("40.00"), new BigDecimal("50.00"),
                new BigDecimal("10.00"), Instant.parse("2026-11-05T10:00:00Z"), null);

        SaleResponse back = codec.read(codec.write(sale), SaleResponse.class);

        assertThat(back).isEqualTo(sale);
    }

    @Test
    void shouldFailClearlyOnCorruptSnapshots() {
        assertThatThrownBy(() -> codec.read("{not json", SaleResponse.class))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SaleResponse");
    }
}

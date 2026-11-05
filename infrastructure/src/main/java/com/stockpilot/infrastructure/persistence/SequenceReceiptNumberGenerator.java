package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.port.out.ReceiptNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class SequenceReceiptNumberGenerator implements ReceiptNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    SequenceReceiptNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String next() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('sale_receipt_seq')", Long.class);
        return String.format("SALE-%06d", value);
    }
}

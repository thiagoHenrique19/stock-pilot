package com.stockpilot.core.port.in.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodRevenue(LocalDate periodStart, long salesCount, BigDecimal revenue, BigDecimal cumulativeRevenue) {
}

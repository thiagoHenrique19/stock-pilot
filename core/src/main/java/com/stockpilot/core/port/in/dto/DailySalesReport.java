package com.stockpilot.core.port.in.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesReport(LocalDate date, int salesCount, BigDecimal revenue, BigDecimal averageTicket) {
}

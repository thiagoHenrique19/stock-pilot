package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.DailySalesReport;
import java.time.LocalDate;

public interface GenerateSalesReportUseCase {

    DailySalesReport dailySummary(LocalDate date);
}

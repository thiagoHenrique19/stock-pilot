package com.stockpilot.core.port.in.dto;

import jakarta.validation.constraints.Min;

public record RestockRequest(@Min(1) int quantity) {
}

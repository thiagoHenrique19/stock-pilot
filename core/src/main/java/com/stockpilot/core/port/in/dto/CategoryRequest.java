package com.stockpilot.core.port.in.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String name, String description) {
}

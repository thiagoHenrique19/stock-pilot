package com.stockpilot.core.port.in.dto;

import com.stockpilot.core.domain.Role;

public record UserResponse(Long id, String email, Role role, boolean active) {
}

package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.LoginRequest;
import com.stockpilot.core.port.in.dto.LoginResponse;

public interface AuthenticateUserUseCase {

    LoginResponse login(LoginRequest request);
}

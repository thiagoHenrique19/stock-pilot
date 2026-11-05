package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.RegisterUserRequest;
import com.stockpilot.core.port.in.dto.UserResponse;

public interface ManageUsersUseCase {

    UserResponse register(RegisterUserRequest request);

    /** Creates the first administrator when no user exists yet. Returns whether a user was created. */
    boolean bootstrapAdmin(String email, String password);
}

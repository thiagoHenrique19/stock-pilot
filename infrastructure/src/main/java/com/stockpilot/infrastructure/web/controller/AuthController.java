package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.AuthenticateUserUseCase;
import com.stockpilot.core.port.in.ManageUsersUseCase;
import com.stockpilot.core.port.in.dto.LoginRequest;
import com.stockpilot.core.port.in.dto.LoginResponse;
import com.stockpilot.core.port.in.dto.RegisterUserRequest;
import com.stockpilot.core.port.in.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticationService;
    private final ManageUsersUseCase userService;

    public AuthController(AuthenticateUserUseCase authenticationService, ManageUsersUseCase userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @Operation(summary = "Authenticate with email and password and receive a bearer token")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @Operation(summary = "Create a user (ADMIN only)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }
}

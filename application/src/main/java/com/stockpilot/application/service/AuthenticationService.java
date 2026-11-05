package com.stockpilot.application.service;

import com.stockpilot.core.domain.User;
import com.stockpilot.core.exception.AuthenticationFailedException;
import com.stockpilot.core.port.in.AuthenticateUserUseCase;
import com.stockpilot.core.port.in.dto.LoginRequest;
import com.stockpilot.core.port.in.dto.LoginResponse;
import com.stockpilot.core.port.out.AccessToken;
import com.stockpilot.core.port.out.PasswordHasher;
import com.stockpilot.core.port.out.TokenIssuer;
import com.stockpilot.core.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService implements AuthenticateUserUseCase {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher,
                                 TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .filter(User::isActive)
                .filter(candidate -> passwordHasher.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));
        AccessToken token = tokenIssuer.issue(user);
        return LoginResponse.bearer(token.value(), token.validity().toSeconds());
    }
}

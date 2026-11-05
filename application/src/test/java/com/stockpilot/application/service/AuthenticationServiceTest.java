package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.stockpilot.core.domain.Role;
import com.stockpilot.core.domain.User;
import com.stockpilot.core.exception.AuthenticationFailedException;
import com.stockpilot.core.port.in.dto.LoginRequest;
import com.stockpilot.core.port.in.dto.LoginResponse;
import com.stockpilot.core.port.out.AccessToken;
import com.stockpilot.core.port.out.PasswordHasher;
import com.stockpilot.core.port.out.TokenIssuer;
import com.stockpilot.core.port.out.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenIssuer tokenIssuer;

    private AuthenticationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userRepository, passwordHasher, tokenIssuer);
        user = User.create("admin@shop.com", "hashed", Role.ADMIN);
    }

    @Test
    void shouldIssueBearerTokenForValidCredentials() {
        when(userRepository.findByEmail("admin@shop.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret123", "hashed")).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(new AccessToken("jwt-value", Duration.ofMinutes(30)));

        LoginResponse response = service.login(new LoginRequest("  Admin@Shop.com ", "secret123"));

        assertThat(response.accessToken()).isEqualTo("jwt-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800);
    }

    @Test
    void shouldRejectWrongPassword() {
        when(userRepository.findByEmail("admin@shop.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin@shop.com", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void shouldRejectUnknownUserWithTheSameMessage() {
        when(userRepository.findByEmail("ghost@shop.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@shop.com", "whatever1")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void shouldRejectInactiveUser() {
        user.deactivate();
        when(userRepository.findByEmail("admin@shop.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin@shop.com", "secret123")))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}

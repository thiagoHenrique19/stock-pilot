package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.core.domain.Role;
import com.stockpilot.core.domain.User;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.port.in.dto.RegisterUserRequest;
import com.stockpilot.core.port.in.dto.UserResponse;
import com.stockpilot.core.port.out.PasswordHasher;
import com.stockpilot.core.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordHasher);
    }

    @Test
    void shouldRegisterUserStoringOnlyTheHash() {
        when(userRepository.existsByEmail("cashier@shop.com")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.register(
                new RegisterUserRequest("Cashier@Shop.com", "password123", Role.CASHIER));

        assertThat(response.email()).isEqualTo("cashier@shop.com");
        assertThat(response.role()).isEqualTo(Role.CASHIER);
        assertThat(response.active()).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        when(userRepository.existsByEmail("cashier@shop.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterUserRequest("cashier@shop.com", "password123", Role.CASHIER)))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldBootstrapAdminWhenNoUsersExist() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordHasher.hash("admin-pass-1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean created = service.bootstrapAdmin("admin@shop.com", "admin-pass-1");

        assertThat(created).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldNotBootstrapWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(1L);

        assertThat(service.bootstrapAdmin("admin@shop.com", "admin-pass-1")).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldNotBootstrapWithoutCredentials() {
        assertThat(service.bootstrapAdmin(null, "x")).isFalse();
        assertThat(service.bootstrapAdmin("admin@shop.com", " ")).isFalse();
        verify(userRepository, never()).save(any());
    }
}

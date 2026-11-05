package com.stockpilot.application.service;

import com.stockpilot.core.domain.Role;
import com.stockpilot.core.domain.User;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.port.in.ManageUsersUseCase;
import com.stockpilot.core.port.in.dto.RegisterUserRequest;
import com.stockpilot.core.port.in.dto.UserResponse;
import com.stockpilot.core.port.out.PasswordHasher;
import com.stockpilot.core.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements ManageUsersUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Email already registered: " + email);
        }
        User user = User.create(email, passwordHasher.hash(request.password()), request.role());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public boolean bootstrapAdmin(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        if (userRepository.count() > 0) {
            return false;
        }
        userRepository.save(User.create(email, passwordHasher.hash(password), Role.ADMIN));
        return true;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive());
    }
}

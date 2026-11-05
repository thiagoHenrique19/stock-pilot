package com.stockpilot.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.AbstractIntegrationTest;
import com.stockpilot.core.domain.Role;
import com.stockpilot.core.domain.User;
import com.stockpilot.core.port.out.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class UserPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndFindUserByEmail() {
        String email = "user-" + UUID.randomUUID() + "@shop.com";
        userRepository.save(User.create(email, "hash", Role.CASHIER));

        User found = userRepository.findByEmail(email).orElseThrow();

        assertThat(found.getRole()).isEqualTo(Role.CASHIER);
        assertThat(found.isActive()).isTrue();
        assertThat(userRepository.existsByEmail(email)).isTrue();
        assertThat(userRepository.existsByEmail("nobody-" + UUID.randomUUID() + "@shop.com")).isFalse();
        assertThat(userRepository.count()).isPositive();
    }

    @Test
    void shouldEnforceUniqueEmail() {
        String email = "dup-" + UUID.randomUUID() + "@shop.com";
        userRepository.save(User.create(email, "hash", Role.ADMIN));

        assertThatThrownBy(() -> userRepository.save(User.create(email, "hash2", Role.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM app_user WHERE email = ?", Long.class, email))
                .isEqualTo(1L);
    }
}

package com.stockpilot.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldNormalizeEmailAndStartActive() {
        User user = User.create("  Jane.Doe@Example.COM ", "hash", Role.CASHIER);

        assertThat(user.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(Role.CASHIER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertThatThrownBy(() -> User.create(" ", "hash", Role.ADMIN)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.create(null, "hash", Role.ADMIN)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.create("not-an-email", "hash", Role.ADMIN))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRequirePasswordHashAndRole() {
        assertThatThrownBy(() -> User.create("a@b.com", " ", Role.ADMIN)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.create("a@b.com", "hash", null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldDeactivateAndReactivate() {
        User user = User.create("a@b.com", "hash", Role.ADMIN);

        user.deactivate();
        assertThat(user.isActive()).isFalse();
        user.activate();
        assertThat(user.isActive()).isTrue();
    }
}

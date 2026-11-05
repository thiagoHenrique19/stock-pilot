package com.stockpilot.core.domain;

import com.stockpilot.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean active = true;

    private Instant createdAt = Instant.now();

    protected User() {
    }

    private User(String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static User create(String email, String passwordHash, Role role) {
        if (email == null || !email.trim().matches("[^@\\s]+@[^@\\s]+")) {
            throw new BusinessRuleException("A valid email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new BusinessRuleException("Password hash is required");
        }
        if (role == null) {
            throw new BusinessRuleException("Role is required");
        }
        return new User(normalizeEmail(email), passwordHash, role);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public void deactivate() {
        active = false;
    }

    public void activate() {
        active = true;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

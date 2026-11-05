package com.stockpilot;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    public static final String ADMIN_EMAIL = "admin@stockpilot.test";
    public static final String ADMIN_PASSWORD = "admin-password-123";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.admin.email", () -> ADMIN_EMAIL);
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
        registry.add("security.jwt.secret", () -> "integration-test-secret-integration-test-secret");
    }
}

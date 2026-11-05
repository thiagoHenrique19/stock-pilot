package com.stockpilot.infrastructure.security;

import com.stockpilot.core.port.in.ManageUsersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the first administrator from APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD when no user exists. */
@Component
class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final ManageUsersUseCase userService;
    private final String email;
    private final String password;

    AdminBootstrapRunner(ManageUsersUseCase userService,
                         @Value("${app.admin.email:}") String email,
                         @Value("${app.admin.password:}") String password) {
        this.userService = userService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userService.bootstrapAdmin(email, password)) {
            log.info("Bootstrap administrator created: {}", email);
        }
    }
}

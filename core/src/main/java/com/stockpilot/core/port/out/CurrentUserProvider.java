package com.stockpilot.core.port.out;

import java.util.Optional;

public interface CurrentUserProvider {

    String SYSTEM_USER = "system";

    /** Identifier (the email) of whoever is making the current request, empty outside an authenticated request. */
    Optional<String> currentUser();

    default String currentUserOrSystem() {
        return currentUser().orElse(SYSTEM_USER);
    }
}

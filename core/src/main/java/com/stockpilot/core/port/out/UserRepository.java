package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long count();
}

package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.User;
import com.stockpilot.core.port.out.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class UserPersistenceAdapter implements UserRepository {

    private final JpaUserRepository jpaRepository;

    UserPersistenceAdapter(JpaUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}

package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Category;
import com.stockpilot.core.port.out.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class CategoryPersistenceAdapter implements CategoryRepository {

    private final JpaCategoryRepository jpaRepository;

    CategoryPersistenceAdapter(JpaCategoryRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Category save(Category category) {
        return jpaRepository.save(category);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll(Sort.by("name"));
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return jpaRepository.existsByNameIgnoreCase(name);
    }
}

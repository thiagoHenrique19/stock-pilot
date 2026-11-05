package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);
}

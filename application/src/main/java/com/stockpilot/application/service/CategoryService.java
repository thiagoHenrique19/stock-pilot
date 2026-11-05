package com.stockpilot.application.service;

import com.stockpilot.application.mapper.CategoryMapper;
import com.stockpilot.core.domain.Category;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.ManageCategoriesUseCase;
import com.stockpilot.core.port.in.dto.CategoryRequest;
import com.stockpilot.core.port.in.dto.CategoryResponse;
import com.stockpilot.core.port.out.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService implements ManageCategoriesUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (request.name() != null && categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessRuleException("Category already exists: " + request.name().trim());
        }
        Category category = Category.create(request.name(), request.description());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }
}

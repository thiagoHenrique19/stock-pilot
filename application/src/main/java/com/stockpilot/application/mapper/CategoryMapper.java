package com.stockpilot.application.mapper;

import com.stockpilot.core.domain.Category;
import com.stockpilot.core.port.in.dto.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}

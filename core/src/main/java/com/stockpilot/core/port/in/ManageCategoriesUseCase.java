package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.CategoryRequest;
import com.stockpilot.core.port.in.dto.CategoryResponse;
import java.util.List;

public interface ManageCategoriesUseCase {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse findById(Long id);

    List<CategoryResponse> listAll();
}

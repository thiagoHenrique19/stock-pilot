package com.stockpilot.infrastructure.web.controller;

import com.stockpilot.core.port.in.ManageCategoriesUseCase;
import com.stockpilot.core.port.in.dto.CategoryRequest;
import com.stockpilot.core.port.in.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ManageCategoriesUseCase categoryService;

    public CategoryController(ManageCategoriesUseCase categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a category (name must be unique)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @Operation(summary = "Get a category by id")
    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    @Operation(summary = "List all categories")
    @GetMapping
    public List<CategoryResponse> listAll() {
        return categoryService.listAll();
    }
}

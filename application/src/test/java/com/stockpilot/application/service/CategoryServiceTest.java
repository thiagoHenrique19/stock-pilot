package com.stockpilot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpilot.application.mapper.CategoryMapper;
import com.stockpilot.core.domain.Category;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.dto.CategoryRequest;
import com.stockpilot.core.port.in.dto.CategoryResponse;
import com.stockpilot.core.port.out.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, new CategoryMapper());
    }

    @Test
    void shouldCreateCategory() {
        when(categoryRepository.existsByNameIgnoreCase("Drinks")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.create(new CategoryRequest("Drinks", "Cold drinks"));

        assertThat(response.name()).isEqualTo("Drinks");
        assertThat(response.description()).isEqualTo("Cold drinks");
    }

    @Test
    void shouldRejectDuplicatedName() {
        when(categoryRepository.existsByNameIgnoreCase("Drinks")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Drinks", null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldFindCategoryById() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.create("Food", null)));

        assertThat(service.findById(1L).name()).isEqualTo("Food");
    }

    @Test
    void shouldFailWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(Category.create("A", null), Category.create("B", null)));

        assertThat(service.listAll()).extracting(CategoryResponse::name).containsExactly("A", "B");
    }
}

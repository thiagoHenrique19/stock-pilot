package com.stockpilot.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import com.stockpilot.core.port.in.ManageCategoriesUseCase;
import com.stockpilot.core.port.in.dto.CategoryResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ManageCategoriesUseCase categoryService;

    private final CategoryResponse drinks = new CategoryResponse(1L, "Drinks", "Cold drinks");

    @Test
    void shouldCreateCategory() throws Exception {
        when(categoryService.create(any())).thenReturn(drinks);

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drinks\",\"description\":\"Cold drinks\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Drinks"));
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnprocessableEntityForDuplicatedName() throws Exception {
        when(categoryService.create(any())).thenThrow(new BusinessRuleException("Category already exists"));

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drinks\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldListCategories() throws Exception {
        when(categoryService.listAll()).thenReturn(List.of(drinks));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Drinks"));
    }

    @Test
    void shouldGetCategoryById() throws Exception {
        when(categoryService.findById(1L)).thenReturn(drinks);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturnNotFoundForUnknownCategory() throws Exception {
        when(categoryService.findById(9L)).thenThrow(new ResourceNotFoundException("Category", 9L));

        mockMvc.perform(get("/api/categories/9")).andExpect(status().isNotFound());
    }
}

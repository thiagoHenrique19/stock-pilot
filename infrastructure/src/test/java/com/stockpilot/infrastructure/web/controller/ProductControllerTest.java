package com.stockpilot.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ManageProductsUseCase productService;

    private final ProductResponse response =
            new ProductResponse(1L, "SKU-1", "Coffee", new BigDecimal("19.90"), 10, 3, false);

    @Test
    void shouldCreateProduct() throws Exception {
        when(productService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-1","name":"Coffee","price":19.90,"quantity":10,"minimumQuantity":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-1"));
    }

    @Test
    void shouldRejectInvalidProduct() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"","name":"","price":-1,"quantity":-1,"minimumQuantity":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListLowStockProducts() throws Exception {
        when(productService.listLowStock()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Coffee"));
    }
}

package com.stockpilot.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.port.in.ExecuteIdempotentlyUseCase;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import com.stockpilot.core.query.SortDirection;
import java.math.BigDecimal;
import java.util.List;
import com.stockpilot.infrastructure.web.support.WithSecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@WithSecurityConfig
@WithMockUser(roles = "ADMIN")
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ManageProductsUseCase productService;
    @MockBean private ExecuteIdempotentlyUseCase idempotency;

    private final ProductResponse response = new ProductResponse(1L, "SKU-1", "Coffee", "Roasted", 2L, "Drinks",
            new BigDecimal("19.90"), 10, 3, false, true);

    @Test
    void shouldCreateProduct() throws Exception {
        when(productService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-1","name":"Coffee","price":19.90,"quantity":10,"minimumQuantity":3,"categoryId":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.categoryName").value("Drinks"));
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
    void shouldUpdateProduct() throws Exception {
        when(productService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Coffee","price":19.90,"minimumQuantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Coffee"));
    }

    @Test
    void shouldRejectInvalidUpdate() throws Exception {
        mockMvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-5,\"minimumQuantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeactivateAndReactivateProduct() throws Exception {
        when(productService.deactivate(1L)).thenReturn(response);
        when(productService.reactivate(1L)).thenReturn(response);

        mockMvc.perform(post("/api/products/1/deactivate")).andExpect(status().isOk());
        mockMvc.perform(post("/api/products/1/reactivate")).andExpect(status().isOk());
        verify(productService).deactivate(1L);
        verify(productService).reactivate(1L);
    }

    @Test
    void shouldSearchProductsWithFiltersPaginationAndSorting() throws Exception {
        when(productService.search(any(), any())).thenReturn(new PageResult<>(List.of(response), 1, 5, 11));

        mockMvc.perform(get("/api/products")
                        .param("name", "cof").param("categoryId", "2").param("active", "true")
                        .param("lowStockOnly", "true").param("page", "1").param("size", "5")
                        .param("sortBy", "price").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Coffee"))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3));

        ArgumentCaptor<ProductSearchCriteria> criteria = ArgumentCaptor.forClass(ProductSearchCriteria.class);
        ArgumentCaptor<PageQuery> pageQuery = ArgumentCaptor.forClass(PageQuery.class);
        verify(productService).search(criteria.capture(), pageQuery.capture());
        org.assertj.core.api.Assertions.assertThat(criteria.getValue())
                .isEqualTo(new ProductSearchCriteria("cof", 2L, true, true));
        org.assertj.core.api.Assertions.assertThat(pageQuery.getValue())
                .isEqualTo(new PageQuery(1, 5, "price", SortDirection.DESC));
    }

    @Test
    void shouldRejectInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldListLowStockProducts() throws Exception {
        when(productService.listLowStock()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Coffee"));
    }

    @Test
    void shouldRestockWithoutIdempotencyKey() throws Exception {
        when(productService.restock(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/products/1/restock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1"));

        verifyNoInteractions(idempotency);
    }

    @Test
    void shouldScopeTheRestockKeyToTheProduct() throws Exception {
        when(idempotency.execute(eq("products.restock:1"), eq("abc"), any(), eq(ProductResponse.class), any()))
                .thenReturn(new ExecuteIdempotentlyUseCase.Result<>(response, true));

        mockMvc.perform(post("/api/products/1/restock").header("Idempotency-Key", "abc")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"));
    }
}

package com.stockpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WithMockUser(roles = "ADMIN")
class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldPublishOpenApiDocumentWithInfoTagsAndPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Stock Pilot API"))
                .andExpect(jsonPath("$.paths['/api/products']").exists())
                .andExpect(jsonPath("$.paths['/api/sales/{id}/cancel']").exists())
                .andExpect(jsonPath("$.paths['/api/reports/top-products']").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Products')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Sales')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Reports')]").exists());
    }

    @Test
    void shouldDescribeEveryOperationWithASummary() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths.*.*[?(!@.summary)]").isEmpty());
    }

    @Test
    void shouldServeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void shouldReturnProblemDetailAndRequestIdHeaderForErrors() throws Exception {
        mockMvc.perform(get("/api/products/999999").header("X-Request-Id", "trace-7"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "trace-7"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.requestId").value("trace-7"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturnValidationErrorsAsProblemDetail() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"name\":\"\",\"price\":-1,\"quantity\":0,\"minimumQuantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'sku')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'price')]").exists())
                .andExpect(header().exists("X-Request-Id"));
    }
}

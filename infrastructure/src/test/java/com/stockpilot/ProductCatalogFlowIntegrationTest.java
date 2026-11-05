package com.stockpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ProductCatalogFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private int createCategory(String name) throws Exception {
        String body = mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int createProduct(String sku, String name, int categoryId, int quantity) throws Exception {
        String body = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"%s","price":10.00,"quantity":%d,"minimumQuantity":2,"categoryId":%d}
                                """.formatted(sku, name, quantity, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void shouldManageCategoriesAndRejectDuplicates() throws Exception {
        String name = "Cat-" + UUID.randomUUID();
        int id = createCategory(name);

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name.toUpperCase())))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/categories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
        mockMvc.perform(get("/api/categories/999999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    void shouldNormalizeSkuAndRejectDuplicatesIgnoringCase() throws Exception {
        String sku = "mix-" + UUID.randomUUID().toString().substring(0, 6);
        int categoryId = createCategory("Cat-" + UUID.randomUUID());
        int productId = createProduct(sku, "Mixed case", categoryId, 5);

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(jsonPath("$.sku").value(sku.toUpperCase()));
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"Again","price":1.00,"quantity":1,"minimumQuantity":0}
                                """.formatted(sku.toUpperCase())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldUpdateDeactivateAndBlockSalesOfInactiveProducts() throws Exception {
        int categoryId = createCategory("Cat-" + UUID.randomUUID());
        int productId = createProduct("UPD-" + UUID.randomUUID().toString().substring(0, 6), "Before", categoryId, 5);

        mockMvc.perform(put("/api/products/" + productId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"After\",\"description\":\"New\",\"price\":12.50,\"minimumQuantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.categoryId").doesNotExist());

        mockMvc.perform(post("/api/products/" + productId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        String sale = """
                {"paymentMethod":"CASH","items":[{"productId":%d,"quantity":1}]}
                """.formatted(productId);
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(sale))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/products/" + productId + "/reactivate"))
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(sale))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldSearchProductsByCategoryNameAndStatus() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        int categoryId = createCategory("Cat-" + tag);
        createProduct("S1-" + tag, tag + " Alpha", categoryId, 10);
        createProduct("S2-" + tag, tag + " Beta", categoryId, 1);
        int inactive = createProduct("S3-" + tag, tag + " Gamma", categoryId, 10);
        mockMvc.perform(post("/api/products/" + inactive + "/deactivate")).andExpect(status().isOk());

        mockMvc.perform(get("/api/products").param("categoryId", String.valueOf(categoryId))
                        .param("active", "true").param("sortBy", "name").param("direction", "desc")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].name").value(tag + " Beta"));

        mockMvc.perform(get("/api/products").param("name", tag.toUpperCase()).param("lowStockOnly", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(tag + " Beta"));

        mockMvc.perform(get("/api/products").param("sortBy", "nonsense")).andExpect(status().isBadRequest());
    }
}

package com.stockpilot.infrastructure.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldMapResourceNotFoundToProblemDetail() throws Exception {
        mockMvc.perform(get("/fail/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Product not found with id 9"))
                .andExpect(jsonPath("$.instance").value("/fail/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldMapBusinessRuleToUnprocessableEntity() throws Exception {
        mockMvc.perform(get("/fail/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.detail").value("Insufficient stock"));
    }

    @Test
    void shouldMapInvalidRequestToBadRequest() throws Exception {
        mockMvc.perform(get("/fail/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Bad paging"));
    }

    @Test
    void shouldMapConcurrentUpdateToConflict() throws Exception {
        mockMvc.perform(get("/fail/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldListFieldErrorsForValidationFailures() throws Exception {
        mockMvc.perform(post("/fail/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'quantity')].message").isNotEmpty());
    }

    @Test
    void shouldMapMalformedBodyMissingAndMistypedParameters() throws Exception {
        mockMvc.perform(post("/fail/validate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/fail/param"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/fail/param").param("value", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldHideInternalDetailsOfUnexpectedErrors() throws Exception {
        mockMvc.perform(get("/fail/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void shouldIncludeRequestIdWhenAvailable() throws Exception {
        MDC.put("requestId", "req-123");
        try {
            mockMvc.perform(get("/fail/not-found"))
                    .andExpect(jsonPath("$.requestId").value("req-123"));
        } finally {
            MDC.clear();
        }
    }

    @RestController
    @RequestMapping("/fail")
    static class FailingController {

        record Payload(@NotBlank String name, @Min(0) int quantity) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Product", 9L);
        }

        @GetMapping("/business")
        void business() {
            throw new BusinessRuleException("Insufficient stock");
        }

        @GetMapping("/invalid")
        void invalid() {
            throw new InvalidRequestException("Bad paging");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ObjectOptimisticLockingFailureException("Sale", 1L);
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret internal detail");
        }

        @GetMapping("/param")
        void param(@RequestParam int value) {
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody Payload payload) {
        }
    }
}

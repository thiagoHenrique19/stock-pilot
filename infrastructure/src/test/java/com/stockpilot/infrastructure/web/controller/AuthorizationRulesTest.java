package com.stockpilot.infrastructure.web.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.stockpilot.core.port.in.AdjustStockUseCase;
import com.stockpilot.core.port.in.AuthenticateUserUseCase;
import com.stockpilot.core.port.in.CheckoutSaleUseCase;
import com.stockpilot.core.port.in.GenerateAnalyticsReportsUseCase;
import com.stockpilot.core.port.in.GenerateSalesReportUseCase;
import com.stockpilot.core.port.in.ManageCategoriesUseCase;
import com.stockpilot.core.port.in.ManageProductsUseCase;
import com.stockpilot.core.port.in.ManageSalesUseCase;
import com.stockpilot.core.port.in.ManageUsersUseCase;
import com.stockpilot.core.port.in.QueryStockMovementsUseCase;
import com.stockpilot.infrastructure.web.support.WithSecurityConfig;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest({ProductController.class, CategoryController.class, SaleController.class, ReportController.class,
        StockMovementController.class, AuthController.class})
@WithSecurityConfig
class AuthorizationRulesTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ManageProductsUseCase products;
    @MockBean private ManageCategoriesUseCase categories;
    @MockBean private CheckoutSaleUseCase checkout;
    @MockBean private ManageSalesUseCase sales;
    @MockBean private GenerateSalesReportUseCase dailyReport;
    @MockBean private GenerateAnalyticsReportsUseCase analytics;
    @MockBean private AdjustStockUseCase adjustments;
    @MockBean private QueryStockMovementsUseCase movements;
    @MockBean private AuthenticateUserUseCase authentication;
    @MockBean private ManageUsersUseCase users;

    private static final String ANY_BODY = "{}";

    static Stream<Arguments> endpoints() {
        return Stream.of(
                // method, path, cashierAllowed
                Arguments.of(HttpMethod.GET, "/api/products", true),
                Arguments.of(HttpMethod.GET, "/api/products/1", true),
                Arguments.of(HttpMethod.GET, "/api/products/low-stock", true),
                Arguments.of(HttpMethod.POST, "/api/sales", true),
                Arguments.of(HttpMethod.POST, "/api/products", false),
                Arguments.of(HttpMethod.PUT, "/api/products/1", false),
                Arguments.of(HttpMethod.POST, "/api/products/1/deactivate", false),
                Arguments.of(HttpMethod.POST, "/api/products/1/reactivate", false),
                Arguments.of(HttpMethod.POST, "/api/products/1/restock", false),
                Arguments.of(HttpMethod.POST, "/api/products/1/adjustments", false),
                Arguments.of(HttpMethod.GET, "/api/products/1/movements", false),
                Arguments.of(HttpMethod.GET, "/api/categories", false),
                Arguments.of(HttpMethod.GET, "/api/categories/1", false),
                Arguments.of(HttpMethod.POST, "/api/categories", false),
                Arguments.of(HttpMethod.GET, "/api/sales", false),
                Arguments.of(HttpMethod.GET, "/api/sales/1", false),
                Arguments.of(HttpMethod.POST, "/api/sales/1/cancel", false),
                Arguments.of(HttpMethod.GET, "/api/reports/daily", false),
                Arguments.of(HttpMethod.GET, "/api/reports/top-products", false),
                Arguments.of(HttpMethod.GET, "/api/reports/revenue", false),
                Arguments.of(HttpMethod.GET, "/api/reports/stock-valuation", false),
                Arguments.of(HttpMethod.GET, "/api/reports/low-stock", false),
                Arguments.of(HttpMethod.POST, "/api/auth/register", false));
    }

    private int statusOf(HttpMethod method, String path, String role) throws Exception {
        var builder = request(method, path).contentType(MediaType.APPLICATION_JSON).content(ANY_BODY);
        if (role != null) {
            builder = builder.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        return result.getResponse().getStatus();
    }

    @ParameterizedTest(name = "{0} {1}: unauthenticated gets 401")
    @MethodSource("endpoints")
    void anonymousIsAlwaysRejected(HttpMethod method, String path, boolean cashierAllowed) throws Exception {
        assertThat(statusOf(method, path, null)).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0} {1}: admin allowed")
    @MethodSource("endpoints")
    void adminIsNeverForbidden(HttpMethod method, String path, boolean cashierAllowed) throws Exception {
        assertThat(statusOf(method, path, "ADMIN")).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "{0} {1}: cashier allowed = {2}")
    @MethodSource("endpoints")
    void cashierIsRestrictedToSalesAndProductReads(HttpMethod method, String path, boolean cashierAllowed)
            throws Exception {
        int status = statusOf(method, path, "CASHIER");

        if (cashierAllowed) {
            assertThat(status).isNotIn(401, 403);
        } else {
            assertThat(status).isEqualTo(403);
        }
    }

    @Test
    void invalidBearerTokenIsRejectedWithProblemDetail() throws Exception {
        var result = mockMvc.perform(request(HttpMethod.GET, "/api/products")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andReturn().getResponse();

        assertThat(result.getStatus()).isEqualTo(401);
        assertThat(result.getContentType()).contains("application/problem+json");
    }

    @Test
    void loginIsPublic() throws Exception {
        assertThat(statusOf(HttpMethod.POST, "/api/auth/login", null)).isNotIn(401, 403);
    }
}

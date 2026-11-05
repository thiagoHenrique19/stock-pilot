package com.stockpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.stockpilot.core.domain.User;
import com.stockpilot.core.port.in.ManageUsersUseCase;
import com.stockpilot.core.port.out.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

class AuthenticationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ManageUsersUseCase userService;
    @Autowired private JwtEncoder jwtEncoder;

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String registerCashier(String adminToken) throws Exception {
        String email = "cashier-" + UUID.randomUUID().toString().substring(0, 8) + "@shop.com";
        mockMvc.perform(post("/api/auth/register").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"cashier-pass-1\",\"role\":\"CASHIER\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CASHIER"));
        return email;
    }

    @Test
    void shouldBootstrapTheAdminOnlyOnce() {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        assertThat(admin.getRole().name()).isEqualTo("ADMIN");
        assertThat(admin.getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(BCrypt.checkpw(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();
        assertThat(userService.bootstrapAdmin("other-admin@shop.com", "another-password")).isFalse();
        assertThat(userRepository.existsByEmail("other-admin@shop.com")).isFalse();
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@shop.com\",\"password\":\"whatever-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestsWithoutValidToken() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/products").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());

        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("https://stock-pilot.local").subject(ADMIN_EMAIL).claim("role", "ADMIN")
                        .issuedAt(Instant.now().minus(Duration.ofHours(3)))
                        .expiresAt(Instant.now().minus(Duration.ofHours(2))).build())).getTokenValue();
        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldKeepPublicEndpointsOpen() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void adminShouldDoEverythingWithARealToken() throws Exception {
        String admin = adminToken();

        mockMvc.perform(post("/api/categories").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Auth-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/reports/stock-valuation").header("Authorization", admin))
                .andExpect(status().isOk());
    }

    @Test
    void cashierShouldOnlySellAndReadProducts() throws Exception {
        String admin = adminToken();
        String email = registerCashier(admin);
        String cashier = login(email, "cashier-pass-1");
        int productId = JsonPath.read(mockMvc.perform(post("/api/products").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"AUTH-%s\",\"name\":\"Auth item\",\"price\":4.00,\"quantity\":10,\"minimumQuantity\":1}"
                                .formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/products/" + productId).header("Authorization", cashier))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products").header("Authorization", cashier)).andExpect(status().isOk());
        mockMvc.perform(post("/api/sales").header("Authorization", cashier).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"PIX\",\"items\":[{\"productId\":%d,\"quantity\":1}]}".formatted(productId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products").header("Authorization", cashier).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"X\",\"name\":\"X\",\"price\":1,\"quantity\":1,\"minimumQuantity\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(post("/api/products/%d/adjustments".formatted(productId)).header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1,\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/categories").header("Authorization", cashier)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/low-stock").header("Authorization", cashier)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/register").header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@shop.com\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/sales/1/cancel").header("Authorization", cashier)).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDuplicatedRegistrationAndInactiveUsers() throws Exception {
        String admin = adminToken();
        String email = registerCashier(admin);

        mockMvc.perform(post("/api/auth/register").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"cashier-pass-1\",\"role\":\"CASHIER\"}"
                                .formatted(email.toUpperCase())))
                .andExpect(status().isUnprocessableEntity());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.deactivate();
        userRepository.save(user);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"cashier-pass-1\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
    }
}

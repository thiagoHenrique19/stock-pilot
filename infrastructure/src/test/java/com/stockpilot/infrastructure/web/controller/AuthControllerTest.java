package com.stockpilot.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpilot.core.domain.Role;
import com.stockpilot.core.exception.AuthenticationFailedException;
import com.stockpilot.core.exception.BusinessRuleException;
import com.stockpilot.core.port.in.AuthenticateUserUseCase;
import com.stockpilot.core.port.in.ManageUsersUseCase;
import com.stockpilot.core.port.in.dto.LoginResponse;
import com.stockpilot.core.port.in.dto.UserResponse;
import com.stockpilot.infrastructure.web.support.WithSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@WithSecurityConfig
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthenticateUserUseCase authenticationService;
    @MockBean private ManageUsersUseCase userService;

    private static final String LOGIN = "{\"email\":\"admin@shop.com\",\"password\":\"secret123\"}";
    private static final String REGISTER = "{\"email\":\"cashier@shop.com\",\"password\":\"password123\",\"role\":\"CASHIER\"}";

    @Test
    void loginShouldBePublicAndReturnBearerToken() throws Exception {
        when(authenticationService.login(any())).thenReturn(LoginResponse.bearer("token-value", 3600));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void loginShouldReturnUnauthorizedProblemForBadCredentials() throws Exception {
        when(authenticationService.login(any())).thenThrow(new AuthenticationFailedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginShouldValidateBody() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldRegisterUsers() throws Exception {
        when(userService.register(any())).thenReturn(new UserResponse(2L, "cashier@shop.com", Role.CASHIER, true));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cashier@shop.com"))
                .andExpect(jsonPath("$.role").value("CASHIER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void registerShouldValidateEmailAndPasswordLength() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"role\":\"CASHIER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void registerShouldReturnUnprocessableEntityForDuplicatedEmail() throws Exception {
        when(userService.register(any())).thenThrow(new BusinessRuleException("Email already registered"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void cashierShouldNotRegisterUsers() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void anonymousShouldNotRegisterUsers() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }
}

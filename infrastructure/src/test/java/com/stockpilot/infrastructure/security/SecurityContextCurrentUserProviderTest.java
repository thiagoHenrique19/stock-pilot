package com.stockpilot.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpilot.core.port.out.CurrentUserProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityContextCurrentUserProviderTest {

    private final CurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBeEmptyWithoutAuthentication() {
        assertThat(provider.currentUser()).isEmpty();
        assertThat(provider.currentUserOrSystem()).isEqualTo("system");
    }

    @Test
    void shouldReturnTheNameOfTheAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "alice@shop.com", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        assertThat(provider.currentUser()).contains("alice@shop.com");
    }

    @Test
    void shouldUseTheJwtSubject() {
        Jwt jwt = new Jwt("t", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", "bob@shop.com"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertThat(provider.currentUserOrSystem()).isEqualTo("bob@shop.com");
    }

    @Test
    void shouldTreatAnonymousAsNobody() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(provider.currentUser()).isEmpty();
    }

    @Test
    void shouldNotLeakTheUserToOtherThreads() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "alice@shop.com", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        assertThat(CompletableFuture.supplyAsync(provider::currentUser).get()).isEmpty();
    }
}

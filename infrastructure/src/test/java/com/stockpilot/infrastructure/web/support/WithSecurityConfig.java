package com.stockpilot.infrastructure.web.support;

import com.stockpilot.infrastructure.security.ProblemDetailSecurityHandlers;
import com.stockpilot.infrastructure.security.SecurityConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Loads the production security rules into a {@code @WebMvcTest} slice. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class, WithSecurityConfig.RejectingJwtDecoderConfig.class})
public @interface WithSecurityConfig {

    @TestConfiguration
    class RejectingJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("Token rejected by test decoder");
            };
        }
    }
}

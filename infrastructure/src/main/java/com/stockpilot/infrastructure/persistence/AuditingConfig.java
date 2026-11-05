package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.port.out.CurrentUserProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AuditingConfig {

    @Bean
    HibernatePropertiesCustomizer auditInterceptorCustomizer(CurrentUserProvider currentUser) {
        AuditInterceptor interceptor = new AuditInterceptor(currentUser);
        return properties -> properties.put("hibernate.session_factory.interceptor", interceptor);
    }
}

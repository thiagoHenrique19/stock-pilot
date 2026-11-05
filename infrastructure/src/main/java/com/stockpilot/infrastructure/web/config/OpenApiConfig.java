package com.stockpilot.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI stockPilotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Pilot API")
                        .version("0.2.0")
                        .description("Inventory and point of sale REST API for small businesses. "
                                + "Errors follow RFC 7807 (application/problem+json) and every response carries an "
                                + "X-Request-Id header for correlation.")
                        .contact(new Contact().name("Stock Pilot").url("https://github.com/thiagoHenrique19/stock-pilot")))
                .tags(List.of(
                        new Tag().name("Products").description("Product catalog and stock levels"),
                        new Tag().name("Categories").description("Product categories"),
                        new Tag().name("Stock").description("Stock adjustments and movement ledger"),
                        new Tag().name("Sales").description("Checkout, cancellation and sales history"),
                        new Tag().name("Reports").description("Sales and inventory analytics")));
    }
}

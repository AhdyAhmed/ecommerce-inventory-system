package com.portfolio.ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc auto-generates the OpenAPI spec from the controllers and DTOs
 * with no configuration at all - this bean only supplies the human-facing
 * metadata (title, description, contact) that appears at the top of
 * /swagger-ui.html, since springdoc has no way to infer that on its own.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecommerceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce / Inventory Management System API")
                        .description("""
                                REST API for a portfolio project covering product catalog management, \
                                dynamic filtering, and order processing with stock control.

                                Built with Spring Boot, Spring Data JPA, and PostgreSQL. See the project \
                                README for the full architecture write-up, design decisions, and a \
                                cumulative verification checklist covering every endpoint below.""")
                        .version("v1.0")
                        .contact(new Contact()
                                // TODO: replace with your actual GitHub repo URL once this is pushed
                                .name("Project repository")
                                .url("https://github.com/YOUR_USERNAME/ecommerce-inventory-system"))
                        .license(new License()
                                .name("Portfolio project - not licensed for production use")));
    }

}

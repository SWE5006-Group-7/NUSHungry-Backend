package com.nushungry.cafeteriaservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for Cafeteria Service
 * Provides Swagger UI documentation for cafeteria and stall management APIs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cafeteriaServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cafeteria Service API")
                        .version("1.0")
                        .description("NUSHungry Cafeteria Service - Manages cafeterias and stalls with Redis caching")
                        .contact(new Contact()
                                .name("NUSHungry Team")
                                .email("support@nushungry.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("NUSHungry Documentation"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from authentication service")));
    }
}



package com.nushungry.cafeteriaservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cafeteriaServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("Cafeteria Service API").version("v1"))
            .externalDocs(new ExternalDocumentation().description("NUSHungry Docs"));
    }
}



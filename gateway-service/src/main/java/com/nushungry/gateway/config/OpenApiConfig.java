package com.nushungry.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI Configuration for Gateway Service
 * Aggregates API documentation from all microservices
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public List<GroupedOpenApi> apis(RouteDefinitionLocator locator) {
        List<GroupedOpenApi> groups = new ArrayList<>();

        // Get all route definitions
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();

        if (definitions != null) {
            definitions.forEach(routeDefinition -> {
                String name = routeDefinition.getId();

                // Create a grouped API for each service
                groups.add(GroupedOpenApi.builder()
                        .group(name)
                        .pathsToMatch("/" + getServicePath(name) + "/**")
                        .build());
            });
        }

        return groups;
    }

    /**
     * Get the base path for each service
     */
    private String getServicePath(String serviceName) {
        switch (serviceName) {
            case "admin-service":
                return "api/admin";
            case "cafeteria-service":
                return "api/cafeterias"; // Also handles /api/stalls
            case "review-service":
                return "api/reviews";
            case "media-service":
                return "media";
            case "preference-service":
                return "api/favorites"; // Also handles /api/search-history
            default:
                return "api";
        }
    }
}

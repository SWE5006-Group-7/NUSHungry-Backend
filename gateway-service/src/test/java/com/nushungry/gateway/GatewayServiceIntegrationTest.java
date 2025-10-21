package com.nushungry.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Gateway Service
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayServiceIntegrationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void contextLoads() {
        assertThat(routeLocator).isNotNull();
    }

    @Test
    void shouldLoadAllRoutes() {
        long routeCount = routeLocator.getRoutes().count().block();
        assertThat(routeCount).isGreaterThan(0);
    }

    @Test
    void shouldHaveAdminServiceRoute() {
        boolean hasAdminRoute = routeLocator.getRoutes()
                .any(route -> "admin-service".equals(route.getId()))
                .block();
        assertThat(hasAdminRoute).isTrue();
    }

    @Test
    void shouldHaveCafeteriaServiceRoute() {
        boolean hasCafeteriaRoute = routeLocator.getRoutes()
                .any(route -> "cafeteria-service".equals(route.getId()))
                .block();
        assertThat(hasCafeteriaRoute).isTrue();
    }

    @Test
    void shouldHaveReviewServiceRoute() {
        boolean hasReviewRoute = routeLocator.getRoutes()
                .any(route -> "review-service".equals(route.getId()))
                .block();
        assertThat(hasReviewRoute).isTrue();
    }

    @Test
    void shouldHaveMediaServiceRoute() {
        boolean hasMediaRoute = routeLocator.getRoutes()
                .any(route -> "media-service".equals(route.getId()))
                .block();
        assertThat(hasMediaRoute).isTrue();
    }

    @Test
    void shouldHavePreferenceServiceRoute() {
        boolean hasPreferenceRoute = routeLocator.getRoutes()
                .any(route -> "preference-service".equals(route.getId()))
                .block();
        assertThat(hasPreferenceRoute).isTrue();
    }
}

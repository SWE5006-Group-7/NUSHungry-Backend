package com.nushungry.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Config Server
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("native")
class ConfigServerApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Verify that application context loads successfully
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void healthEndpointShouldBeAccessibleWithoutAuthentication() {
        // Health endpoint should be publicly accessible
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void configEndpointShouldRequireAuthentication() {
        // Config endpoints should require authentication
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/application/default", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void configEndpointShouldBeAccessibleWithAuthentication() {
        // Config endpoints should be accessible with valid credentials
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("config", "config123")
                .getForEntity("http://localhost:" + port + "/application/default", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldLoadNativeConfiguration() {
        // Should be able to load configuration from native profile
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("config", "config123")
                .getForEntity("http://localhost:" + port + "/application/native", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

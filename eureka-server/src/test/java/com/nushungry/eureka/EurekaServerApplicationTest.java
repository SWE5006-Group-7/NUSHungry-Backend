package com.nushungry.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Eureka Server Application
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
class EurekaServerApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }

    @Test
    void eurekaServerIsRunning() {
        // Test that Eureka server is accessible
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("eureka", "eureka")
            .getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void eurekaDashboardIsAccessible() {
        // Test that Eureka dashboard is accessible
        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("eureka", "eureka")
            .getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuatorInfoEndpoint() {
        // Test actuator info endpoint
        String url = "http://localhost:" + port + "/actuator/info";
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("eureka", "eureka")
            .getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

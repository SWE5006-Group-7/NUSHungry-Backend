package com.nushungry.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Service Application
 *
 * This service acts as the single entry point for all client requests,
 * routing them to appropriate microservices and providing cross-cutting
 * concerns like authentication, rate limiting, and CORS.
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}

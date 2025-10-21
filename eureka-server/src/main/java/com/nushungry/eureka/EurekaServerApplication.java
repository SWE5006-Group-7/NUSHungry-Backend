package com.nushungry.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server Application
 * Service Registry for NUSHungry Microservices Architecture
 *
 * Provides:
 * - Service discovery and registration
 * - Health monitoring
 * - Load balancing support
 * - Dashboard UI at http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}

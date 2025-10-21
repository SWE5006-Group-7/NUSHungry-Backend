package com.nushungry.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server Application
 *
 * Centralized configuration management server for all NUSHungry microservices.
 *
 * Features:
 * - Git-backed configuration storage
 * - Environment-specific configurations (dev, staging, prod)
 * - Configuration refresh without service restart
 * - Encryption/decryption of sensitive properties
 * - Version control and audit trail
 *
 * Port: 8888 (default Spring Cloud Config Server port)
 *
 * @author NUSHungry Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

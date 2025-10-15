package com.nushungry;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared integration test configuration to force an embedded H2 database,
 * bypassing any environment-provided datasource settings.
 */
public abstract class IntegrationTestBase {

    private static final String H2_URL = "jdbc:h2:mem:nushungry_test;" +
            "MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.defer-datasource-initialization", () -> "true");
        registry.add("spring.sql.init.mode", () -> "always");

        registry.add("jwt.secret", () -> "test-secret-key");
        registry.add("jwt.expiration", () -> "3600000");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "2525");

        registry.add("file.storage.upload-dir", () -> "build/uploads");
    }
}

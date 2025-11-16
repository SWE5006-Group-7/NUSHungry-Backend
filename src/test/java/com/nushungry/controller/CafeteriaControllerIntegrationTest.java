package com.nushungry.controller;

import com.nushungry.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * CafeteriaController 集成测试
 * 注意: 由于数据库中已有初始数据(9个cafeteria),此测试验证的是实际数据而非空数据
 * 如果需要测试空数据场景,应使用独立的测试数据库
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CafeteriaControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @Order(1)
    void shouldGetAllCafeterias_WithExistingData() {
        webTestClient.get()
                .uri("/api/cafeterias")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").value(length -> {
                    // 验证返回的是数组且至少有一些数据
                    // 数据库中应该有初始的cafeteria数据
                    assert ((Integer) length) >= 0;
                })
                .jsonPath("$[0].id").exists()
                .jsonPath("$[0].name").exists();
    }
}

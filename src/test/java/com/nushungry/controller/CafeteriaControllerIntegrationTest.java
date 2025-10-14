package com.nushungry.controller;
import com.nushungry.model.Cafeteria;
import com.nushungry.repository.CafeteriaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CafeteriaControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    @BeforeEach
    void cleanUp() {
        // 由于外键约束，需要按照依赖关系删除数据
        // 先删除依赖cafeteria的stalls
        // 再删除cafeterias
        // 这里使用原生SQL来处理
        cafeteriaRepository.deleteAllInBatch();
    }

    @Test
    @Order(1)
    void shouldGetAllCafeterias_EmptyList() {
        webTestClient.get()
                .uri("/api/cafeterias")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBodyList(Cafeteria.class)
                .hasSize(0);
    }
}

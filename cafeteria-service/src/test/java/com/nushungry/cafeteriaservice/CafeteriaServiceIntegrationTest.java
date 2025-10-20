package com.nushungry.cafeteriaservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.CafeteriaRepository;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CafeteriaServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    @Autowired
    private StallRepository stallRepository;

    @BeforeEach
    void setUp() {
        stallRepository.deleteAll();
        cafeteriaRepository.deleteAll();
    }

    @Test
    void testCompleteCafeteriaWorkflow() throws Exception {
        // 1. 创建食堂
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Test Cafeteria");
        cafeteria.setLocation("Test Location");

        String createResponse = mockMvc.perform(post("/api/admin/cafeterias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cafeteria)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cafeteria.name").value("Test Cafeteria"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cafeteriaId = objectMapper.readTree(createResponse).get("cafeteria").get("id").asLong();

        // 2. 查询所有食堂
        mockMvc.perform(get("/api/cafeterias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Test Cafeteria"));

        // 3. 查询单个食堂
        mockMvc.perform(get("/api/cafeterias/" + cafeteriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Cafeteria"))
                .andExpect(jsonPath("$.location").value("Test Location"));

        // 4. 更新食堂
        cafeteria.setName("Updated Cafeteria");
        mockMvc.perform(put("/api/admin/cafeterias/" + cafeteriaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cafeteria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cafeteria.name").value("Updated Cafeteria"));

        // 5. 删除食堂
        mockMvc.perform(delete("/api/admin/cafeterias/" + cafeteriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 6. 验证删除
        mockMvc.perform(get("/api/cafeterias/" + cafeteriaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testStallWorkflow() throws Exception {
        // 创建食堂
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Test Cafeteria");
        cafeteria.setLocation("Test Location");
        cafeteria = cafeteriaRepository.save(cafeteria);

        // 创建档口
        Stall stall1 = new Stall();
        stall1.setName("Stall 1");
        stall1.setCuisineType("Chinese");
        stall1.setCafeteria(cafeteria);
        stallRepository.save(stall1);

        Stall stall2 = new Stall();
        stall2.setName("Stall 2");
        stall2.setCuisineType("Western");
        stall2.setCafeteria(cafeteria);
        stallRepository.save(stall2);

        // 查询所有档口
        mockMvc.perform(get("/api/stalls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 查询单个档口
        mockMvc.perform(get("/api/stalls/" + stall1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Stall 1"))
                .andExpect(jsonPath("$.cuisineType").value("Chinese"));
    }

    @Test
    void testSearchFunctionality() throws Exception {
        // 创建测试数据
        Cafeteria cafeteria1 = new Cafeteria();
        cafeteria1.setName("North Spine");
        cafeteria1.setLocation("North");
        cafeteriaRepository.save(cafeteria1);

        Cafeteria cafeteria2 = new Cafeteria();
        cafeteria2.setName("South Spine");
        cafeteria2.setLocation("South");
        cafeteriaRepository.save(cafeteria2);

        // 搜索测试
        mockMvc.perform(get("/api/cafeterias/search")
                        .param("keyword", "North"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", containsString("North")));
    }

    @Test
    void testRatingUpdate() throws Exception {
        // 创建食堂和档口
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Test Cafeteria");
        cafeteria.setLocation("Test Location");
        cafeteria = cafeteriaRepository.save(cafeteria);

        Stall stall = new Stall();
        stall.setName("Test Stall");
        stall.setCafeteria(cafeteria);
        stall.setAvgRating(0.0);
        stall = stallRepository.save(stall);

        // 模拟评分更新（通常由 RabbitMQ 事件触发）
        stall.setAvgRating(4.5);
        stallRepository.save(stall);

        // 验证评分更新
        mockMvc.perform(get("/api/stalls/" + stall.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(4.5));
    }

    @Test
    void testNotFoundScenarios() throws Exception {
        // 测试不存在的食堂
        mockMvc.perform(get("/api/cafeterias/999"))
                .andExpect(status().isNotFound());

        // 测试不存在的档口
        mockMvc.perform(get("/api/stalls/999"))
                .andExpect(status().isNotFound());

        // 测试更新不存在的食堂
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Test");
        mockMvc.perform(put("/api/admin/cafeterias/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cafeteria)))
                .andExpect(status().isNotFound());
    }
}

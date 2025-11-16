package com.nushungry.mediaservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSingleImageUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.fileName").exists())
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    void testImageUpload_PngFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    void testImageUpload_LargeFile() throws Exception {
        // 创建一个较大的文件（1MB）
        byte[] largeContent = new byte[1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        mockMvc.perform(multipart("/api/upload/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(largeContent.length));
    }

    @Test
    void testImageUpload_EmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // 空文件也可以上传（业务逻辑可能需要验证）
        mockMvc.perform(multipart("/api/upload/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

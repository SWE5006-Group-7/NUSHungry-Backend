package com.nushungry.mediaservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        mockMvc.perform(multipart("/media/upload").file(file))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.fileName", endsWith("test.jpg")));
    }
}
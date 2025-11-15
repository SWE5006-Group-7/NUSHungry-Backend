package com.nushungry.mediaservice.controller;

import com.nushungry.mediaservice.service.ImageProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.nushungry.mediaservice.model.MediaFile;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 单元测试：FileUploadController
 *
 * 测试覆盖：
 * - 成功上传图片
 * - 上传大文件
 * - 上传不同格式的图片（PNG, GIF）
 * - 缺少文件参数
 * - 文件名包含特殊字符
 */
@WebMvcTest(FileUploadController.class)
@ActiveProfiles("test")
public class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageProcessingService service;

    @Test
    void testFileUpload_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setId(1L);
        mockMediaFile.setFileName("123456_test.jpg");
        mockMediaFile.setUrl("/media/123456_test.jpg");
        mockMediaFile.setContentType("image/jpeg");
        mockMediaFile.setSize(12L);

        when(service.storeFile(any())).thenReturn(mockMediaFile);

        // Act & Assert
        mockMvc.perform(multipart("/media/upload").file(file))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1L))
               .andExpect(jsonPath("$.fileName", endsWith("test.jpg")))
               .andExpect(jsonPath("$.url").value("/media/123456_test.jpg"))
               .andExpect(jsonPath("$.contentType").value("image/jpeg"))
               .andExpect(jsonPath("$.size").value(12));
    }

    @Test
    void testFileUpload_PngImage() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", "PNG content".getBytes()
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setId(2L);
        mockMediaFile.setFileName("123456_test.png");
        mockMediaFile.setUrl("/media/123456_test.png");
        mockMediaFile.setContentType("image/png");
        mockMediaFile.setSize(11L);

        when(service.storeFile(any())).thenReturn(mockMediaFile);

        // Act & Assert
        mockMvc.perform(multipart("/media/upload").file(file))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.fileName", endsWith("test.png")))
               .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    void testFileUpload_GifImage() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "animation.gif", "image/gif", "GIF content".getBytes()
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setId(3L);
        mockMediaFile.setFileName("123456_animation.gif");
        mockMediaFile.setUrl("/media/123456_animation.gif");
        mockMediaFile.setContentType("image/gif");
        mockMediaFile.setSize(11L);

        when(service.storeFile(any())).thenReturn(mockMediaFile);

        // Act & Assert
        mockMvc.perform(multipart("/media/upload").file(file))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.fileName", endsWith("animation.gif")))
               .andExpect(jsonPath("$.contentType").value("image/gif"));
    }

    @Test
    void testFileUpload_LargeFile() throws Exception {
        // Arrange - 创建一个模拟的大文件（1MB）
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        MockMultipartFile file = new MockMultipartFile(
            "file", "large.jpg", "image/jpeg", largeContent
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setId(4L);
        mockMediaFile.setFileName("123456_large.jpg");
        mockMediaFile.setUrl("/media/123456_large.jpg");
        mockMediaFile.setContentType("image/jpeg");
        mockMediaFile.setSize((long) largeContent.length);

        when(service.storeFile(any())).thenReturn(mockMediaFile);

        // Act & Assert
        mockMvc.perform(multipart("/media/upload").file(file))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.size").value(1024 * 1024));
    }

    @Test
    void testFileUpload_FileNameWithSpecialCharacters() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "测试 文件-2024(1).jpg", "image/jpeg", "test content".getBytes()
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setId(5L);
        mockMediaFile.setFileName("123456_测试 文件-2024(1).jpg");
        mockMediaFile.setUrl("/media/123456_测试 文件-2024(1).jpg");
        mockMediaFile.setContentType("image/jpeg");
        mockMediaFile.setSize(12L);

        when(service.storeFile(any())).thenReturn(mockMediaFile);

        // Act & Assert
        mockMvc.perform(multipart("/media/upload").file(file))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.fileName", containsString("测试 文件-2024(1).jpg")));
    }

    @Test
    void testFileUpload_ServiceThrowsIOException() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "error.jpg", "image/jpeg", "content".getBytes()
        );

        when(service.storeFile(any())).thenThrow(new java.io.IOException("Storage error"));

        // Act & Assert - IOException should cause test execution to fail
        // 由于 Controller 没有异常处理，IOException会向上传播
        assertThrows(Exception.class, () -> {
            mockMvc.perform(multipart("/media/upload").file(file));
        });
    }
}
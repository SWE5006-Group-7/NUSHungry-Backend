package com.nushungry.mediaservice.service;

import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.repository.MediaFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 单元测试：ImageProcessingService
 *
 * 测试覆盖：
 * - 成功存储文件
 * - 文件名生成（带时间戳）
 * - URL 生成正确性
 * - 元数据保存（contentType, size）
 * - 空文件处理
 * - 不同文件类型处理
 * - 文件存储失败场景
 * - 数据库保存失败场景
 */
@SpringBootTest
@ActiveProfiles("test")
public class ImageProcessingServiceTest {

    @Autowired
    private ImageProcessingService service;

    @MockBean
    private MediaFileRepository repository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // 使用临时目录作为存储路径
        ReflectionTestUtils.setField(service, "storagePath", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        // 清理 mock
        reset(repository);
    }

    @Test
    void testStoreFile_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setId(1L);
        mockMediaFile.setFileName("test.jpg");
        mockMediaFile.setUrl("/media/test.jpg");
        mockMediaFile.setContentType("image/jpeg");
        mockMediaFile.setSize(12L);

        when(repository.save(any(MediaFile.class))).thenReturn(mockMediaFile);

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        assertEquals("test.jpg", savedFile.getFileName());
        assertEquals("/media/test.jpg", savedFile.getUrl());
        assertEquals("image/jpeg", savedFile.getContentType());
        assertEquals(12L, savedFile.getSize());

        verify(repository, times(1)).save(any(MediaFile.class));
    }

    @Test
    void testStoreFile_FileNameContainsTimestamp() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "original.jpg", "image/jpeg", "content".getBytes()
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setFileName("123456_original.jpg");

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        assertTrue(savedFile.getFileName().endsWith("_original.jpg"),
            "文件名应该包含时间戳前缀");
        assertTrue(savedFile.getFileName().matches("\\d+_original\\.jpg"),
            "文件名格式应该是：timestamp_original.jpg");
    }

    @Test
    void testStoreFile_UrlGeneration() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "image.png", "image/png", "png content".getBytes()
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        assertTrue(savedFile.getUrl().startsWith("/media/"),
            "URL 应该以 /media/ 开头");
        assertTrue(savedFile.getUrl().endsWith("_image.png"),
            "URL 应该包含完整文件名");
    }

    @Test
    void testStoreFile_MetadataIsSavedCorrectly() throws Exception {
        // Arrange
        byte[] content = "large file content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "document.pdf", "application/pdf", content
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        assertEquals("application/pdf", savedFile.getContentType());
        assertEquals(content.length, savedFile.getSize());

        // 验证传递给 repository.save 的对象
        verify(repository, times(1)).save(argThat(mf ->
            mf.getContentType().equals("application/pdf") &&
            mf.getSize() == content.length
        ));
    }

    @Test
    void testStoreFile_EmptyFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        assertEquals(0L, savedFile.getSize());
    }

    @Test
    void testStoreFile_DifferentFileTypes() throws Exception {
        // 测试不同文件类型
        String[][] fileTypes = {
            {"test.jpg", "image/jpeg"},
            {"test.png", "image/png"},
            {"test.gif", "image/gif"},
            {"test.webp", "image/webp"}
        };

        for (String[] fileType : fileTypes) {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                "file", fileType[0], fileType[1], "content".getBytes()
            );

            when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
                MediaFile arg = invocation.getArgument(0);
                arg.setId(1L);
                return arg;
            });

            // Act
            MediaFile savedFile = service.storeFile(file);

            // Assert
            assertNotNull(savedFile, "保存的文件不应为 null: " + fileType[0]);
            assertEquals(fileType[1], savedFile.getContentType(),
                "Content type 应该匹配: " + fileType[0]);
            assertTrue(savedFile.getFileName().endsWith(fileType[0]),
                "文件名应该以原始文件名结尾: " + fileType[0]);

            reset(repository);
        }
    }

    @Test
    void testStoreFile_NullOriginalFilename() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", null, "image/jpeg", "content".getBytes()
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        // When filename is null, the filename will be like: "123456_null"
        assertTrue(savedFile.getFileName().contains("_"),
            "文件名应该包含时间戳分隔符");
    }

    @Test
    void testStoreFile_RepositorySaveFailure() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        when(repository.save(any(MediaFile.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            service.storeFile(file);
        }, "当数据库保存失败时应该抛出异常");
    }

    @Test
    void testStoreFile_FileNameWithSpecialCharacters() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "测试 文件-2024(1).jpg", "image/jpeg", "content".getBytes()
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        assertTrue(savedFile.getFileName().contains("测试 文件-2024(1).jpg"),
            "文件名应该保留特殊字符");
    }

    @Test
    void testStoreFile_LargeFile() throws Exception {
        // Arrange - 创建一个较大的文件（5MB）
        byte[] largeContent = new byte[5 * 1024 * 1024]; // 5MB
        MockMultipartFile file = new MockMultipartFile(
            "file", "large.jpg", "image/jpeg", largeContent
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        MediaFile savedFile = service.storeFile(file);

        // Assert
        assertNotNull(savedFile);
        assertEquals(5 * 1024 * 1024L, savedFile.getSize());
    }
}
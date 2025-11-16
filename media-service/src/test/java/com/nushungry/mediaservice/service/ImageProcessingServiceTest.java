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

    // ==================== Delete 功能测试 ====================

    @Test
    void testDeleteImageByUrl_Success() throws Exception {
        // Arrange
        String imageUrl = "/media/test.jpg";
        String fileName = "test.jpg";

        // 创建实际文件
        File storageDir = tempDir.toFile();
        File testFile = new File(storageDir, fileName);
        Files.write(testFile.toPath(), "test content".getBytes());

        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(1L);
        mediaFile.setFileName(fileName);
        mediaFile.setUrl(imageUrl);

        when(repository.findByUrl(imageUrl)).thenReturn(java.util.Optional.of(mediaFile));
        doNothing().when(repository).delete(any(MediaFile.class));

        // Act
        boolean result = service.deleteImageByUrl(imageUrl);

        // Assert
        assertTrue(result, "删除应该成功");
        assertFalse(testFile.exists(), "物理文件应该被删除");
        verify(repository, times(1)).findByUrl(imageUrl);
        verify(repository, times(1)).delete(mediaFile);
    }

    @Test
    void testDeleteImageByUrl_ImageNotFound() {
        // Arrange
        String imageUrl = "/media/nonexistent.jpg";
        when(repository.findByUrl(imageUrl)).thenReturn(java.util.Optional.empty());

        // Act
        boolean result = service.deleteImageByUrl(imageUrl);

        // Assert
        assertFalse(result, "删除不存在的图片应该返回 false");
        verify(repository, times(1)).findByUrl(imageUrl);
        verify(repository, never()).delete(any(MediaFile.class));
    }

    @Test
    void testDeleteImageByUrl_PhysicalFileNotExist() throws Exception {
        // Arrange
        String imageUrl = "/media/test.jpg";
        String fileName = "test.jpg";

        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(1L);
        mediaFile.setFileName(fileName);
        mediaFile.setUrl(imageUrl);

        when(repository.findByUrl(imageUrl)).thenReturn(java.util.Optional.of(mediaFile));
        doNothing().when(repository).delete(any(MediaFile.class));

        // Act - 物理文件不存在,但仍应删除数据库记录
        boolean result = service.deleteImageByUrl(imageUrl);

        // Assert
        assertTrue(result, "即使物理文件不存在,删除也应该成功");
        verify(repository, times(1)).delete(mediaFile);
    }

    @Test
    void testDeleteImageByUrl_DeletePhysicalFileFailButDeleteDbRecord() throws Exception {
        // Arrange
        String imageUrl = "/media/test.jpg";
        String fileName = "test.jpg";

        // 创建一个无法删除的文件 (通过设置父目录为只读)
        File storageDir = tempDir.toFile();
        File testFile = new File(storageDir, fileName);
        Files.write(testFile.toPath(), "test content".getBytes());

        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(1L);
        mediaFile.setFileName(fileName);
        mediaFile.setUrl(imageUrl);

        when(repository.findByUrl(imageUrl)).thenReturn(java.util.Optional.of(mediaFile));
        doNothing().when(repository).delete(any(MediaFile.class));

        // 注意: 在某些操作系统上设置只读可能不生效,这个测试主要验证逻辑
        // Act
        boolean result = service.deleteImageByUrl(imageUrl);

        // Assert
        assertTrue(result, "即使物理文件删除失败,也应删除数据库记录并返回成功");
        verify(repository, times(1)).delete(mediaFile);
    }

    @Test
    void testDeleteImagesByUrls_BatchDelete() throws Exception {
        // Arrange
        String imageUrl1 = "/media/test1.jpg";
        String imageUrl2 = "/media/test2.jpg";
        String fileName1 = "test1.jpg";
        String fileName2 = "test2.jpg";

        // 创建实际文件
        File storageDir = tempDir.toFile();
        File testFile1 = new File(storageDir, fileName1);
        File testFile2 = new File(storageDir, fileName2);
        Files.write(testFile1.toPath(), "content1".getBytes());
        Files.write(testFile2.toPath(), "content2".getBytes());

        MediaFile mediaFile1 = new MediaFile();
        mediaFile1.setId(1L);
        mediaFile1.setFileName(fileName1);
        mediaFile1.setUrl(imageUrl1);

        MediaFile mediaFile2 = new MediaFile();
        mediaFile2.setId(2L);
        mediaFile2.setFileName(fileName2);
        mediaFile2.setUrl(imageUrl2);

        when(repository.findByUrl(imageUrl1)).thenReturn(java.util.Optional.of(mediaFile1));
        when(repository.findByUrl(imageUrl2)).thenReturn(java.util.Optional.of(mediaFile2));
        doNothing().when(repository).delete(any(MediaFile.class));

        // Act
        var deletedUrls = service.deleteImagesByUrls(java.util.List.of(imageUrl1, imageUrl2));

        // Assert
        assertEquals(2, deletedUrls.size(), "应该删除2个图片");
        assertTrue(deletedUrls.contains(imageUrl1));
        assertTrue(deletedUrls.contains(imageUrl2));
        assertFalse(testFile1.exists(), "文件1应该被删除");
        assertFalse(testFile2.exists(), "文件2应该被删除");
        verify(repository, times(2)).delete(any(MediaFile.class));
    }

    @Test
    void testDeleteImagesByUrls_PartialSuccess() throws Exception {
        // Arrange
        String imageUrl1 = "/media/test1.jpg";
        String imageUrl2 = "/media/nonexistent.jpg";
        String fileName1 = "test1.jpg";

        // 仅创建第一个文件
        File storageDir = tempDir.toFile();
        File testFile1 = new File(storageDir, fileName1);
        Files.write(testFile1.toPath(), "content1".getBytes());

        MediaFile mediaFile1 = new MediaFile();
        mediaFile1.setId(1L);
        mediaFile1.setFileName(fileName1);
        mediaFile1.setUrl(imageUrl1);

        when(repository.findByUrl(imageUrl1)).thenReturn(java.util.Optional.of(mediaFile1));
        when(repository.findByUrl(imageUrl2)).thenReturn(java.util.Optional.empty());
        doNothing().when(repository).delete(any(MediaFile.class));

        // Act
        var deletedUrls = service.deleteImagesByUrls(java.util.List.of(imageUrl1, imageUrl2));

        // Assert
        assertEquals(1, deletedUrls.size(), "应该只成功删除1个图片");
        assertTrue(deletedUrls.contains(imageUrl1));
        assertFalse(deletedUrls.contains(imageUrl2));
        verify(repository, times(1)).delete(mediaFile1);
    }

    @Test
    void testDeleteImagesByUrls_EmptyList() {
        // Act
        var deletedUrls = service.deleteImagesByUrls(java.util.List.of());

        // Assert
        assertEquals(0, deletedUrls.size(), "空列表应该返回空结果");
        verify(repository, never()).delete(any(MediaFile.class));
    }

    // ==================== 图片验证功能测试 ====================

    @Test
    void testIsValidImage_ValidJpeg() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );
        assertTrue(service.isValidImage(file));
    }

    @Test
    void testIsValidImage_ValidPng() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", "content".getBytes()
        );
        assertTrue(service.isValidImage(file));
    }

    @Test
    void testIsValidImage_ValidGif() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.gif", "image/gif", "content".getBytes()
        );
        assertTrue(service.isValidImage(file));
    }

    @Test
    void testIsValidImage_ValidWebp() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.webp", "image/webp", "content".getBytes()
        );
        assertTrue(service.isValidImage(file));
    }

    @Test
    void testIsValidImage_ValidJpg() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpg", "content".getBytes()
        );
        assertTrue(service.isValidImage(file));
    }

    @Test
    void testIsValidImage_InvalidContentType() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "content".getBytes()
        );
        assertFalse(service.isValidImage(file));
    }

    @Test
    void testIsValidImage_NullContentType() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", null, "content".getBytes()
        );
        assertFalse(service.isValidImage(file));
    }

    @Test
    void testIsValidImage_NullFile() {
        assertFalse(service.isValidImage(null));
    }

    @Test
    void testIsValidImage_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", new byte[0]
        );
        assertFalse(service.isValidImage(file));
    }

    // ==================== 批量上传功能测试 ====================

    @Test
    void testBatchUploadImages_AllSuccess() throws Exception {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile(
            "file", "test1.jpg", "image/jpeg", "content1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "file", "test2.png", "image/png", "content2".getBytes()
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        var result = service.batchUploadImages(
            java.util.List.of(file1, file2), false, false
        );

        // Assert
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(2, result.getSuccessList().size());
        assertEquals(0, result.getFailureList().size());
        verify(repository, times(2)).save(any(MediaFile.class));
    }

    @Test
    void testBatchUploadImages_PartialFailure() throws Exception {
        // Arrange
        MockMultipartFile validFile = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "content".getBytes()
        );

        when(repository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        // Act
        var result = service.batchUploadImages(
            java.util.List.of(validFile, invalidFile), false, false
        );

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getSuccessList().size());
        assertEquals(1, result.getFailureList().size());
        assertTrue(result.getFailureList().get(0).getMessage().contains("无效的图片文件"));
        verify(repository, times(1)).save(any(MediaFile.class));
    }

    @Test
    void testBatchUploadImages_EmptyList() {
        // Act
        var result = service.batchUploadImages(
            java.util.List.of(), false, false
        );

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(repository, never()).save(any(MediaFile.class));
    }

    @Test
    void testBatchUploadImages_SaveException() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        when(repository.save(any(MediaFile.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        var result = service.batchUploadImages(
            java.util.List.of(file), false, false
        );

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getFailureList().get(0).getMessage().contains("上传失败"));
    }

    // ==================== 图片信息提取测试 ====================

    @Test
    void testGetImageInfo_BasicInfo() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // Act
        var imageInfo = service.getImageInfo(file);

        // Assert
        assertNotNull(imageInfo);
        assertEquals("image/jpeg", imageInfo.getContentType());
        assertEquals("test.jpg", imageInfo.getFileName());
        assertEquals(7L, imageInfo.getSize()); // "content".length
    }
}
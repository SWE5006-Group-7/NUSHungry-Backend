package com.nushungry.mediaservice.repository;

import com.nushungry.mediaservice.model.MediaFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：MediaFileRepository
 *
 * 测试覆盖：
 * - 基本 CRUD 操作
 * - 数据持久化
 * - 查询所有文件
 * - 按 ID 查询
 * - 删除操作
 * - 空表场景
 */
@DataJpaTest
@ActiveProfiles("test")
public class MediaFileRepositoryTest {

    @Autowired
    private MediaFileRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private MediaFile testMediaFile1;
    private MediaFile testMediaFile2;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        testMediaFile1 = new MediaFile();
        testMediaFile1.setFileName("test1.jpg");
        testMediaFile1.setUrl("/media/test1.jpg");
        testMediaFile1.setContentType("image/jpeg");
        testMediaFile1.setSize(1024L);

        testMediaFile2 = new MediaFile();
        testMediaFile2.setFileName("test2.png");
        testMediaFile2.setUrl("/media/test2.png");
        testMediaFile2.setContentType("image/png");
        testMediaFile2.setSize(2048L);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        repository.deleteAll();
        entityManager.clear();
    }

    @Test
    void testSaveMediaFile() {
        // Act
        MediaFile saved = repository.save(testMediaFile1);

        // Assert
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("test1.jpg", saved.getFileName());
        assertEquals("/media/test1.jpg", saved.getUrl());
        assertEquals("image/jpeg", saved.getContentType());
        assertEquals(1024L, saved.getSize());
    }

    @Test
    void testFindById_Found() {
        // Arrange
        MediaFile saved = entityManager.persistAndFlush(testMediaFile1);
        Long id = saved.getId();

        // Act
        Optional<MediaFile> found = repository.findById(id);

        // Assert
        assertTrue(found.isPresent());
        assertEquals("test1.jpg", found.get().getFileName());
        assertEquals("/media/test1.jpg", found.get().getUrl());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Optional<MediaFile> found = repository.findById(999L);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
        // Arrange
        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);

        // Act
        List<MediaFile> allFiles = repository.findAll();

        // Assert
        assertEquals(2, allFiles.size());
        assertTrue(allFiles.stream().anyMatch(f -> f.getFileName().equals("test1.jpg")));
        assertTrue(allFiles.stream().anyMatch(f -> f.getFileName().equals("test2.png")));
    }

    @Test
    void testFindAll_EmptyTable() {
        // Act
        List<MediaFile> allFiles = repository.findAll();

        // Assert
        assertEquals(0, allFiles.size());
    }

    @Test
    void testDeleteById() {
        // Arrange
        MediaFile saved = entityManager.persistAndFlush(testMediaFile1);
        Long id = saved.getId();

        // Act
        repository.deleteById(id);
        entityManager.flush();

        // Assert
        Optional<MediaFile> found = repository.findById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void testDeleteAll() {
        // Arrange
        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);

        // Act
        repository.deleteAll();
        entityManager.flush();

        // Assert
        List<MediaFile> allFiles = repository.findAll();
        assertEquals(0, allFiles.size());
    }

    @Test
    void testUpdateMediaFile() {
        // Arrange
        MediaFile saved = entityManager.persistAndFlush(testMediaFile1);
        Long id = saved.getId();

        // Act - 更新文件名
        saved.setFileName("updated.jpg");
        saved.setUrl("/media/updated.jpg");
        MediaFile updated = repository.save(saved);
        entityManager.flush();

        // Assert
        Optional<MediaFile> found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals("updated.jpg", found.get().getFileName());
        assertEquals("/media/updated.jpg", found.get().getUrl());
    }

    @Test
    void testCount() {
        // Arrange
        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);

        // Act
        long count = repository.count();

        // Assert
        assertEquals(2, count);
    }

    @Test
    void testExistsById_True() {
        // Arrange
        MediaFile saved = entityManager.persistAndFlush(testMediaFile1);
        Long id = saved.getId();

        // Act
        boolean exists = repository.existsById(id);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Act
        boolean exists = repository.existsById(999L);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testSaveMultipleFiles() {
        // Arrange
        MediaFile file3 = new MediaFile();
        file3.setFileName("test3.gif");
        file3.setUrl("/media/test3.gif");
        file3.setContentType("image/gif");
        file3.setSize(3072L);

        // Act
        repository.save(testMediaFile1);
        repository.save(testMediaFile2);
        repository.save(file3);
        entityManager.flush();

        // Assert
        List<MediaFile> allFiles = repository.findAll();
        assertEquals(3, allFiles.size());
    }

    @Test
    void testPersistenceOfMetadata() {
        // Arrange
        testMediaFile1.setContentType("application/pdf");
        testMediaFile1.setSize(1024 * 1024L); // 1MB

        // Act
        MediaFile saved = repository.save(testMediaFile1);
        entityManager.flush();
        entityManager.clear(); // 清除缓存，强制从数据库读取

        // Assert
        Optional<MediaFile> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("application/pdf", found.get().getContentType());
        assertEquals(1024 * 1024L, found.get().getSize());
    }
}

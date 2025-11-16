package com.nushungry.mediaservice.repository;

import com.nushungry.mediaservice.model.MediaFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
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

    // ==================== 增强测试：自定义查询方法 ====================

    @Test
    void testFindByUrl_Found() {
        // Arrange
        entityManager.persistAndFlush(testMediaFile1);

        // Act
        Optional<MediaFile> found = repository.findByUrl("/media/test1.jpg");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("test1.jpg", found.get().getFileName());
        assertEquals("/media/test1.jpg", found.get().getUrl());
    }

    @Test
    void testFindByUrl_NotFound() {
        // Act
        Optional<MediaFile> found = repository.findByUrl("/media/nonexistent.jpg");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testFindByUrl_EmptyUrl() {
        // Act
        Optional<MediaFile> found = repository.findByUrl("");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testCountByType() {
        // Arrange
        testMediaFile1.setType("PHOTO");
        testMediaFile2.setType("PHOTO");

        MediaFile menuFile = new MediaFile();
        menuFile.setFileName("menu.jpg");
        menuFile.setUrl("/media/menu.jpg");
        menuFile.setContentType("image/jpeg");
        menuFile.setSize(512L);
        menuFile.setType("MENU");

        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);
        entityManager.persistAndFlush(menuFile);

        // Act
        long photoCount = repository.countByType("PHOTO");
        long menuCount = repository.countByType("MENU");
        long avatarCount = repository.countByType("AVATAR");

        // Assert
        assertEquals(2, photoCount);
        assertEquals(1, menuCount);
        assertEquals(0, avatarCount);
    }

    @Test
    void testCountByUploadedBy() {
        // Arrange
        testMediaFile1.setUploadedBy("user1");
        testMediaFile2.setUploadedBy("user2");

        MediaFile file3 = new MediaFile();
        file3.setFileName("test3.jpg");
        file3.setUrl("/media/test3.jpg");
        file3.setContentType("image/jpeg");
        file3.setSize(512L);
        file3.setUploadedBy("user1");

        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);
        entityManager.persistAndFlush(file3);

        // Act
        long user1Count = repository.countByUploadedBy("user1");
        long user2Count = repository.countByUploadedBy("user2");

        // Assert
        assertEquals(2, user1Count);
        assertEquals(1, user2Count);
    }

    @Test
    void testCountByCreatedAtBetween() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);

        // Act
        long count = repository.countByCreatedAtBetween(yesterday, tomorrow);

        // Assert
        assertEquals(2, count);
    }

    @Test
    void testCountByCreatedAtBetween_NoResults() {
        // Arrange
        LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(2);

        entityManager.persistAndFlush(testMediaFile1);

        // Act
        long count = repository.countByCreatedAtBetween(futureStart, futureEnd);

        // Assert
        assertEquals(0, count);
    }

    @Test
    void testGetTotalSize() {
        // Arrange
        testMediaFile1.setSize(1000L);
        testMediaFile2.setSize(2000L);

        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);

        // Act
        Long totalSize = repository.getTotalSize();

        // Assert
        assertNotNull(totalSize);
        assertEquals(3000L, totalSize);
    }

    @Test
    void testGetTotalSize_EmptyTable() {
        // Act
        Long totalSize = repository.getTotalSize();

        // Assert
        // 空表时可能返回 null 或 0
        assertTrue(totalSize == null || totalSize == 0L);
    }

    @Test
    void testCountByTypeGroupBy() {
        // Arrange
        testMediaFile1.setType("PHOTO");
        testMediaFile2.setType("MENU");

        MediaFile file3 = new MediaFile();
        file3.setFileName("test3.jpg");
        file3.setUrl("/media/test3.jpg");
        file3.setContentType("image/jpeg");
        file3.setSize(512L);
        file3.setType("PHOTO");

        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);
        entityManager.persistAndFlush(file3);

        // Act
        List<Object[]> results = repository.countByTypeGroupBy();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        // 验证统计结果
        boolean foundPhoto = false;
        boolean foundMenu = false;

        for (Object[] row : results) {
            String type = (String) row[0];
            Long count = (Long) row[1];

            if ("PHOTO".equals(type)) {
                assertEquals(2L, count);
                foundPhoto = true;
            } else if ("MENU".equals(type)) {
                assertEquals(1L, count);
                foundMenu = true;
            }
        }

        assertTrue(foundPhoto);
        assertTrue(foundMenu);
    }

    @Test
    void testCountByUploadedByGroupBy() {
        // Arrange
        testMediaFile1.setUploadedBy("user1");
        testMediaFile2.setUploadedBy("user2");

        MediaFile file3 = new MediaFile();
        file3.setFileName("test3.jpg");
        file3.setUrl("/media/test3.jpg");
        file3.setContentType("image/jpeg");
        file3.setSize(512L);
        file3.setUploadedBy("user1");

        entityManager.persistAndFlush(testMediaFile1);
        entityManager.persistAndFlush(testMediaFile2);
        entityManager.persistAndFlush(file3);

        // Act
        List<Object[]> results = repository.countByUploadedByGroupBy();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        // 验证统计结果
        boolean foundUser1 = false;
        boolean foundUser2 = false;

        for (Object[] row : results) {
            String user = (String) row[0];
            Long count = (Long) row[1];

            if ("user1".equals(user)) {
                assertEquals(2L, count);
                foundUser1 = true;
            } else if ("user2".equals(user)) {
                assertEquals(1L, count);
                foundUser2 = true;
            }
        }

        assertTrue(foundUser1);
        assertTrue(foundUser2);
    }

    @Test
    void testAutoTimestamps() {
        // Act
        MediaFile saved = repository.save(testMediaFile1);
        entityManager.flush();

        // Assert
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        // createdAt 和 updatedAt 可能有微小差异,只验证它们都被设置了
        assertTrue(saved.getCreatedAt().isBefore(saved.getUpdatedAt())
                || saved.getCreatedAt().equals(saved.getUpdatedAt()),
                "更新时间应该大于等于创建时间");
    }

    @Test
    void testUpdateTimestamp() throws InterruptedException {
        // Arrange
        MediaFile saved = repository.save(testMediaFile1);
        entityManager.flush();
        LocalDateTime createdAt = saved.getCreatedAt();

        // 等待一小段时间确保时间戳不同
        Thread.sleep(10);

        // Act - 更新文件
        saved.setFileName("updated.jpg");
        MediaFile updated = repository.save(saved);
        entityManager.flush();

        // Assert
        assertEquals(createdAt, updated.getCreatedAt(), "创建时间不应改变");
        assertTrue(updated.getUpdatedAt().isAfter(createdAt) || updated.getUpdatedAt().equals(createdAt),
                "更新时间应该大于等于创建时间");
    }
}

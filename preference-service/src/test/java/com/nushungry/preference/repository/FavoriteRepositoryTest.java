package com.nushungry.preference.repository;

import com.nushungry.preference.entity.Favorite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("FavoriteRepository Custom Query Tests")
class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Long testUserId1 = 1L;
    private Long testUserId2 = 2L;
    private Long stallId1 = 101L;
    private Long stallId2 = 102L;
    private Long stallId3 = 103L;
    private Long stallId4 = 104L;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should batch delete favorites by userId and stallIds")
    void testDeleteByUserIdAndStallIdIn_NormalScenario() {
        // Arrange
        Favorite favorite1 = createFavorite(testUserId1, stallId1);
        Favorite favorite2 = createFavorite(testUserId1, stallId2);
        Favorite favorite3 = createFavorite(testUserId1, stallId3);
        Favorite favorite4 = createFavorite(testUserId1, stallId4);
        
        repository.saveAll(Arrays.asList(favorite1, favorite2, favorite3, favorite4));
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Long> stallIdsToDelete = Arrays.asList(stallId1, stallId3);
        repository.deleteByUserIdAndStallIdIn(testUserId1, stallIdsToDelete);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<Favorite> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(2);
        assertThat(remaining)
            .extracting(Favorite::getStallId)
            .containsExactlyInAnyOrder(stallId2, stallId4);
    }

    @Test
    @DisplayName("Should handle empty stallId list gracefully")
    void testDeleteByUserIdAndStallIdIn_EmptyList() {
        // Arrange
        Favorite favorite1 = createFavorite(testUserId1, stallId1);
        Favorite favorite2 = createFavorite(testUserId1, stallId2);
        
        repository.saveAll(Arrays.asList(favorite1, favorite2));
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserIdAndStallIdIn(testUserId1, Arrays.asList());
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<Favorite> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(2);
    }

    @Test
    @DisplayName("Should not delete favorites of other users")
    void testDeleteByUserIdAndStallIdIn_IsolateUsers() {
        // Arrange
        Favorite user1Favorite1 = createFavorite(testUserId1, stallId1);
        Favorite user1Favorite2 = createFavorite(testUserId1, stallId2);
        Favorite user2Favorite1 = createFavorite(testUserId2, stallId1);
        Favorite user2Favorite2 = createFavorite(testUserId2, stallId2);
        
        repository.saveAll(Arrays.asList(user1Favorite1, user1Favorite2, user2Favorite1, user2Favorite2));
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserIdAndStallIdIn(testUserId1, Arrays.asList(stallId1));
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<Favorite> user1Remaining = repository.findByUserId(testUserId1);
        List<Favorite> user2Remaining = repository.findByUserId(testUserId2);
        
        assertThat(user1Remaining).hasSize(1);
        assertThat(user1Remaining.get(0).getStallId()).isEqualTo(stallId2);
        assertThat(user2Remaining).hasSize(2);
    }

    @Test
    @DisplayName("Should handle non-existent stallIds gracefully")
    void testDeleteByUserIdAndStallIdIn_NonExistentStallIds() {
        // Arrange
        Favorite favorite1 = createFavorite(testUserId1, stallId1);
        
        repository.save(favorite1);
        entityManager.flush();
        entityManager.clear();

        // Act
        Long nonExistentStallId = 999L;
        repository.deleteByUserIdAndStallIdIn(testUserId1, Arrays.asList(nonExistentStallId));
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<Favorite> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(1);
    }

    @Test
    @DisplayName("Should delete all favorites when all stallIds provided")
    void testDeleteByUserIdAndStallIdIn_DeleteAll() {
        // Arrange
        Favorite favorite1 = createFavorite(testUserId1, stallId1);
        Favorite favorite2 = createFavorite(testUserId1, stallId2);
        Favorite favorite3 = createFavorite(testUserId1, stallId3);
        
        repository.saveAll(Arrays.asList(favorite1, favorite2, favorite3));
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserIdAndStallIdIn(testUserId1, Arrays.asList(stallId1, stallId2, stallId3));
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<Favorite> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("Should verify unique constraint behavior")
    void testUniqueConstraint_UserIdAndStallId() {
        // Arrange
        Favorite favorite = createFavorite(testUserId1, stallId1);
        repository.save(favorite);
        entityManager.flush();
        entityManager.clear();

        // Assert
        boolean exists = repository.existsByUserIdAndStallId(testUserId1, stallId1);
        assertThat(exists).isTrue();
        
        boolean notExists = repository.existsByUserIdAndStallId(testUserId1, stallId2);
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should handle batch delete with large number of stallIds")
    void testDeleteByUserIdAndStallIdIn_LargeBatch() {
        // Arrange
        List<Favorite> favorites = Arrays.asList(
            createFavorite(testUserId1, 1L),
            createFavorite(testUserId1, 2L),
            createFavorite(testUserId1, 3L),
            createFavorite(testUserId1, 4L),
            createFavorite(testUserId1, 5L),
            createFavorite(testUserId1, 6L),
            createFavorite(testUserId1, 7L),
            createFavorite(testUserId1, 8L),
            createFavorite(testUserId1, 9L),
            createFavorite(testUserId1, 10L)
        );
        
        repository.saveAll(favorites);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Long> stallIdsToDelete = Arrays.asList(1L, 3L, 5L, 7L, 9L);
        repository.deleteByUserIdAndStallIdIn(testUserId1, stallIdsToDelete);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<Favorite> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(5);
        assertThat(remaining)
            .extracting(Favorite::getStallId)
            .containsExactlyInAnyOrder(2L, 4L, 6L, 8L, 10L);
    }

    @Test
    @DisplayName("Should handle deleting from empty table gracefully")
    void testDeleteByUserIdAndStallIdIn_EmptyTable() {
        // Act
        repository.deleteByUserIdAndStallIdIn(testUserId1, Arrays.asList(stallId1));
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<Favorite> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).isEmpty();
    }

    private Favorite createFavorite(Long userId, Long stallId) {
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setStallId(stallId);
        favorite.setCreatedAt(System.currentTimeMillis());
        return favorite;
    }
}

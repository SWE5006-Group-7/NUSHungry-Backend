package com.nushungry.preference.repository;

import com.nushungry.preference.entity.SearchHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SearchHistoryRepository Custom Query Tests")
class SearchHistoryRepositoryTest {

    @Autowired
    private SearchHistoryRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Long testUserId1 = 1L;
    private Long testUserId2 = 2L;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should batch delete search history by userId and keywords")
    void testDeleteByUserIdAndKeywordIn_NormalScenario() {
        // Arrange
        SearchHistory history1 = createSearchHistory(testUserId1, "chicken rice");
        SearchHistory history2 = createSearchHistory(testUserId1, "nasi lemak");
        SearchHistory history3 = createSearchHistory(testUserId1, "laksa");
        SearchHistory history4 = createSearchHistory(testUserId1, "mee goreng");
        
        repository.saveAll(Arrays.asList(history1, history2, history3, history4));
        entityManager.flush();
        entityManager.clear();

        // Act
        List<String> keywordsToDelete = Arrays.asList("chicken rice", "laksa");
        repository.deleteByUserIdAndKeywordIn(testUserId1, keywordsToDelete);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(2);
        assertThat(remaining)
            .extracting(SearchHistory::getKeyword)
            .containsExactlyInAnyOrder("nasi lemak", "mee goreng");
    }

    @Test
    @DisplayName("Should handle empty keyword list gracefully")
    void testDeleteByUserIdAndKeywordIn_EmptyList() {
        // Arrange
        SearchHistory history1 = createSearchHistory(testUserId1, "chicken rice");
        SearchHistory history2 = createSearchHistory(testUserId1, "nasi lemak");
        
        repository.saveAll(Arrays.asList(history1, history2));
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserIdAndKeywordIn(testUserId1, Arrays.asList());
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(2);
    }

    @Test
    @DisplayName("Should not delete records of other users")
    void testDeleteByUserIdAndKeywordIn_IsolateUsers() {
        // Arrange
        SearchHistory user1History1 = createSearchHistory(testUserId1, "chicken rice");
        SearchHistory user1History2 = createSearchHistory(testUserId1, "laksa");
        SearchHistory user2History1 = createSearchHistory(testUserId2, "chicken rice");
        SearchHistory user2History2 = createSearchHistory(testUserId2, "laksa");
        
        repository.saveAll(Arrays.asList(user1History1, user1History2, user2History1, user2History2));
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserIdAndKeywordIn(testUserId1, Arrays.asList("chicken rice"));
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> user1Remaining = repository.findByUserId(testUserId1);
        List<SearchHistory> user2Remaining = repository.findByUserId(testUserId2);
        
        assertThat(user1Remaining).hasSize(1);
        assertThat(user1Remaining.get(0).getKeyword()).isEqualTo("laksa");
        assertThat(user2Remaining).hasSize(2);
    }

    @Test
    @DisplayName("Should handle non-existent keywords gracefully")
    void testDeleteByUserIdAndKeywordIn_NonExistentKeywords() {
        // Arrange
        SearchHistory history1 = createSearchHistory(testUserId1, "chicken rice");
        
        repository.save(history1);
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserIdAndKeywordIn(testUserId1, Arrays.asList("non-existent"));
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(1);
    }

    @Test
    @DisplayName("Should delete all search history for a user")
    void testDeleteByUserId_NormalScenario() {
        // Arrange
        SearchHistory history1 = createSearchHistory(testUserId1, "chicken rice");
        SearchHistory history2 = createSearchHistory(testUserId1, "nasi lemak");
        SearchHistory history3 = createSearchHistory(testUserId1, "laksa");
        
        repository.saveAll(Arrays.asList(history1, history2, history3));
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserId(testUserId1);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("Should only delete specified user's search history")
    void testDeleteByUserId_IsolateUsers() {
        // Arrange
        SearchHistory user1History1 = createSearchHistory(testUserId1, "chicken rice");
        SearchHistory user1History2 = createSearchHistory(testUserId1, "laksa");
        SearchHistory user2History1 = createSearchHistory(testUserId2, "chicken rice");
        SearchHistory user2History2 = createSearchHistory(testUserId2, "laksa");
        
        repository.saveAll(Arrays.asList(user1History1, user1History2, user2History1, user2History2));
        entityManager.flush();
        entityManager.clear();

        // Act
        repository.deleteByUserId(testUserId1);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> user1Remaining = repository.findByUserId(testUserId1);
        List<SearchHistory> user2Remaining = repository.findByUserId(testUserId2);
        
        assertThat(user1Remaining).isEmpty();
        assertThat(user2Remaining).hasSize(2);
    }

    @Test
    @DisplayName("Should handle deleting non-existent user's history gracefully")
    void testDeleteByUserId_NonExistentUser() {
        // Arrange
        SearchHistory history1 = createSearchHistory(testUserId1, "chicken rice");
        
        repository.save(history1);
        entityManager.flush();
        entityManager.clear();

        // Act
        Long nonExistentUserId = 999L;
        repository.deleteByUserId(nonExistentUserId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).hasSize(1);
    }

    @Test
    @DisplayName("Should handle deleting from empty table gracefully")
    void testDeleteByUserId_EmptyTable() {
        // Act
        repository.deleteByUserId(testUserId1);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<SearchHistory> remaining = repository.findByUserId(testUserId1);
        assertThat(remaining).isEmpty();
    }

    private SearchHistory createSearchHistory(Long userId, String keyword) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setSearchTime(LocalDateTime.now());
        return history;
    }
}

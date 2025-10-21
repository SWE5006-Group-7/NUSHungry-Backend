package com.nushungry.repository;

import com.nushungry.model.SearchHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchHistoryRepository 测试类
 * 重点测试 @Query 注解的原生 SQL 查询和聚合查询
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SearchHistoryRepository 自定义查询测试")
class SearchHistoryRepositoryTest {

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        // 清空数据库
        searchHistoryRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    // ==================== 测试原生 SQL 查询：findDistinctKeywordsByUserId ====================

    @Test
    @DisplayName("原生SQL查询 - 查找用户唯一关键词（去重） - 正常场景")
    void testFindDistinctKeywordsByUserId_Success() {
        // Given: 用户有多条重复的搜索记录
        createSearchHistory(USER_ID_1, "鸡饭", LocalDateTime.now().minusHours(5));
        createSearchHistory(USER_ID_1, "面条", LocalDateTime.now().minusHours(4));
        createSearchHistory(USER_ID_1, "鸡饭", LocalDateTime.now().minusHours(3)); // 重复
        createSearchHistory(USER_ID_1, "咖啡", LocalDateTime.now().minusHours(2));
        createSearchHistory(USER_ID_1, "鸡饭", LocalDateTime.now().minusHours(1)); // 重复
        entityManager.flush();

        // When: 查询用户的唯一关键词（限制3条）
        List<String> keywords = searchHistoryRepository.findDistinctKeywordsByUserId(USER_ID_1, 3);

        // Then: 应返回3个去重后的关键词，按最近搜索时间排序
        assertThat(keywords).hasSize(3);
        assertThat(keywords).containsExactly("鸡饭", "咖啡", "面条");
    }

    @Test
    @DisplayName("原生SQL查询 - 查找用户唯一关键词 - 空结果场景")
    void testFindDistinctKeywordsByUserId_EmptyResult() {
        // Given: 用户没有搜索记录
        Long nonExistentUserId = 9999L;

        // When: 查询不存在的用户
        List<String> keywords = searchHistoryRepository.findDistinctKeywordsByUserId(nonExistentUserId, 10);

        // Then: 应返回空列表
        assertThat(keywords).isEmpty();
    }

    @Test
    @DisplayName("原生SQL查询 - 查找用户唯一关键词 - 用户隔离测试")
    void testFindDistinctKeywordsByUserId_UserIsolation() {
        // Given: 两个用户有不同的搜索记录
        createSearchHistory(USER_ID_1, "鸡饭", LocalDateTime.now().minusHours(2));
        createSearchHistory(USER_ID_1, "面条", LocalDateTime.now().minusHours(1));
        createSearchHistory(USER_ID_2, "咖啡", LocalDateTime.now().minusHours(2));
        createSearchHistory(USER_ID_2, "奶茶", LocalDateTime.now().minusHours(1));
        entityManager.flush();

        // When: 查询用户1的关键词
        List<String> keywordsUser1 = searchHistoryRepository.findDistinctKeywordsByUserId(USER_ID_1, 10);
        List<String> keywordsUser2 = searchHistoryRepository.findDistinctKeywordsByUserId(USER_ID_2, 10);

        // Then: 每个用户只能看到自己的搜索记录
        assertThat(keywordsUser1).containsExactly("面条", "鸡饭");
        assertThat(keywordsUser2).containsExactly("奶茶", "咖啡");
    }

    @Test
    @DisplayName("原生SQL查询 - 查找用户唯一关键词 - LIMIT 功能测试")
    void testFindDistinctKeywordsByUserId_LimitWorks() {
        // Given: 用户有5个不同的搜索关键词
        createSearchHistory(USER_ID_1, "关键词1", LocalDateTime.now().minusHours(5));
        createSearchHistory(USER_ID_1, "关键词2", LocalDateTime.now().minusHours(4));
        createSearchHistory(USER_ID_1, "关键词3", LocalDateTime.now().minusHours(3));
        createSearchHistory(USER_ID_1, "关键词4", LocalDateTime.now().minusHours(2));
        createSearchHistory(USER_ID_1, "关键词5", LocalDateTime.now().minusHours(1));
        entityManager.flush();

        // When: 限制返回2条
        List<String> keywords = searchHistoryRepository.findDistinctKeywordsByUserId(USER_ID_1, 2);

        // Then: 应只返回最近的2条
        assertThat(keywords).hasSize(2);
        assertThat(keywords).containsExactly("关键词5", "关键词4");
    }

    @Test
    @DisplayName("原生SQL查询 - 查找用户唯一关键词 - 排序正确性（按最近时间）")
    void testFindDistinctKeywordsByUserId_SortOrder() {
        // Given: 同一关键词在不同时间搜索
        createSearchHistory(USER_ID_1, "鸡饭", LocalDateTime.now().minusHours(10)); // 最早
        createSearchHistory(USER_ID_1, "面条", LocalDateTime.now().minusHours(5));
        createSearchHistory(USER_ID_1, "鸡饭", LocalDateTime.now().minusHours(1)); // 最近
        entityManager.flush();

        // When: 查询关键词
        List<String> keywords = searchHistoryRepository.findDistinctKeywordsByUserId(USER_ID_1, 10);

        // Then: "鸡饭"应排在第一位（按MAX(search_time)排序）
        assertThat(keywords).containsExactly("鸡饭", "面条");
    }

    @Test
    @DisplayName("原生SQL查询 - 查找用户唯一关键词 - 特殊字符处理")
    void testFindDistinctKeywordsByUserId_SpecialCharacters() {
        // Given: 包含特殊字符的关键词
        createSearchHistory(USER_ID_1, "McDonald's", LocalDateTime.now().minusHours(3));
        createSearchHistory(USER_ID_1, "面条 & 饺子", LocalDateTime.now().minusHours(2));
        createSearchHistory(USER_ID_1, "咖啡 100%", LocalDateTime.now().minusHours(1));
        entityManager.flush();

        // When: 查询关键词
        List<String> keywords = searchHistoryRepository.findDistinctKeywordsByUserId(USER_ID_1, 10);

        // Then: 应正确返回所有关键词（包含特殊字符）
        assertThat(keywords).hasSize(3);
        assertThat(keywords).contains("McDonald's", "面条 & 饺子", "咖啡 100%");
    }

    // ==================== 测试聚合查询：findPopularKeywords ====================

    @Test
    @DisplayName("聚合查询 - 查找热门关键词 - 正常场景")
    void testFindPopularKeywords_Success() {
        // Given: 多个用户搜索不同的关键词
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(7);

        // "鸡饭" 被搜索 3 次
        createSearchHistory(USER_ID_1, "鸡饭", now.minusHours(5));
        createSearchHistory(USER_ID_2, "鸡饭", now.minusHours(3));
        createSearchHistory(USER_ID_1, "鸡饭", now.minusHours(1));

        // "面条" 被搜索 2 次
        createSearchHistory(USER_ID_2, "面条", now.minusHours(4));
        createSearchHistory(USER_ID_1, "面条", now.minusHours(2));

        // "咖啡" 被搜索 1 次
        createSearchHistory(USER_ID_2, "咖啡", now.minusHours(6));

        // 超过7天的记录（应被过滤）
        createSearchHistory(USER_ID_1, "旧记录", now.minusDays(8));

        entityManager.flush();

        // When: 查询热门关键词（限制前2个）
        List<Object[]> popularKeywords = searchHistoryRepository.findPopularKeywords(since, PageRequest.of(0, 2));

        // Then: 应返回按搜索次数降序排列的关键词
        assertThat(popularKeywords).hasSize(2);

        Object[] first = popularKeywords.get(0);
        assertThat(first[0]).isEqualTo("鸡饭");
        assertThat(first[1]).isEqualTo(3L);

        Object[] second = popularKeywords.get(1);
        assertThat(second[0]).isEqualTo("面条");
        assertThat(second[1]).isEqualTo(2L);
    }

    @Test
    @DisplayName("聚合查询 - 查找热门关键词 - 空结果场景")
    void testFindPopularKeywords_EmptyResult() {
        // Given: 没有符合条件的搜索记录
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        // When: 查询热门关键词
        List<Object[]> popularKeywords = searchHistoryRepository.findPopularKeywords(since, PageRequest.of(0, 10));

        // Then: 应返回空列表
        assertThat(popularKeywords).isEmpty();
    }

    @Test
    @DisplayName("聚合查询 - 查找热门关键词 - 时间过滤测试")
    void testFindPopularKeywords_TimeFilter() {
        // Given: 不同时间的搜索记录
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(3);

        createSearchHistory(USER_ID_1, "新关键词", now.minusDays(1)); // 3天内
        createSearchHistory(USER_ID_1, "旧关键词", now.minusDays(5)); // 5天前

        entityManager.flush();

        // When: 查询最近3天的热门关键词
        List<Object[]> popularKeywords = searchHistoryRepository.findPopularKeywords(since, PageRequest.of(0, 10));

        // Then: 只应返回3天内的关键词
        assertThat(popularKeywords).hasSize(1);
        assertThat(popularKeywords.get(0)[0]).isEqualTo("新关键词");
    }

    @Test
    @DisplayName("聚合查询 - 查找热门关键词 - 分页功能测试")
    void testFindPopularKeywords_Pagination() {
        // Given: 5个不同的关键词
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(7);

        for (int i = 1; i <= 5; i++) {
            for (int j = 0; j < i; j++) {
                createSearchHistory(USER_ID_1, "关键词" + i, now.minusHours(i));
            }
        }
        entityManager.flush();

        // When: 查询第一页（2条）
        List<Object[]> page1 = searchHistoryRepository.findPopularKeywords(since, PageRequest.of(0, 2));

        // Then: 应返回搜索次数最多的前2个
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0)[0]).isEqualTo("关键词5"); // 5次
        assertThat(page1.get(1)[0]).isEqualTo("关键词4"); // 4次
    }

    @Test
    @DisplayName("聚合查询 - 查找热门关键词 - 返回结构验证")
    void testFindPopularKeywords_ReturnStructure() {
        // Given: 一条搜索记录
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(7);
        createSearchHistory(USER_ID_1, "鸡饭", now.minusHours(1));
        entityManager.flush();

        // When: 查询热门关键词
        List<Object[]> popularKeywords = searchHistoryRepository.findPopularKeywords(since, PageRequest.of(0, 10));

        // Then: 验证返回的 Object[] 结构
        assertThat(popularKeywords).hasSize(1);

        Object[] result = popularKeywords.get(0);
        assertThat(result).hasSize(2);
        assertThat(result[0]).isInstanceOf(String.class); // keyword
        assertThat(result[1]).isInstanceOf(Long.class);   // count
    }

    // ==================== 辅助方法 ====================

    private SearchHistory createSearchHistory(Long userId, String keyword, LocalDateTime searchTime) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setSearchTime(searchTime);
        history.setSearchType("stall");
        history.setResultCount(10);
        history.setIpAddress("192.168.1.1");
        return entityManager.persist(history);
    }
}

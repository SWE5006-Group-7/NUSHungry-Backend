package com.nushungry.repository;

import com.nushungry.model.Review;
import com.nushungry.model.Stall;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReviewRepository 测试类
 * 重点测试 @Query 注解的聚合查询和动态排序查询
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ReviewRepository 自定义查询测试")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Stall stall1;
    private Stall stall2;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 清空数据库
        reviewRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // 创建测试用户
        user1 = createUser("user1", "user1@test.com");
        user2 = createUser("user2", "user2@test.com");

        // 创建测试档口
        stall1 = createStall("档口1");
        stall2 = createStall("档口2");

        entityManager.flush();
        entityManager.clear();
    }

    // ==================== 测试聚合查询：getAverageRatingByStallId ====================

    @Test
    @DisplayName("聚合查询 - 计算档口平均评分 - 正常场景")
    void testGetAverageRatingByStallId_Success() {
        // Given: 档口有多条评价
        createReview(stall1, user1, 5.0, "很好", LocalDateTime.now().minusDays(3));
        createReview(stall1, user2, 3.0, "一般", LocalDateTime.now().minusDays(2));
        createReview(stall1, user1, 4.0, "不错", LocalDateTime.now().minusDays(1));
        entityManager.flush();

        // When: 查询平均评分
        Double avgRating = reviewRepository.getAverageRatingByStallId(stall1.getId());

        // Then: 平均评分应为 (5.0 + 3.0 + 4.0) / 3 = 4.0
        assertThat(avgRating).isNotNull();
        assertThat(avgRating).isEqualTo(4.0);
    }

    @Test
    @DisplayName("聚合查询 - 计算档口平均评分 - 空数据返回null")
    void testGetAverageRatingByStallId_NoReviews() {
        // Given: 档口没有评价
        // When: 查询平均评分
        Double avgRating = reviewRepository.getAverageRatingByStallId(stall1.getId());

        // Then: 应返回 null
        assertThat(avgRating).isNull();
    }

    @Test
    @DisplayName("聚合查询 - 计算档口平均评分 - 单条数据")
    void testGetAverageRatingByStallId_SingleReview() {
        // Given: 档口只有一条评价
        createReview(stall1, user1, 4.5, "好吃", LocalDateTime.now());
        entityManager.flush();

        // When: 查询平均评分
        Double avgRating = reviewRepository.getAverageRatingByStallId(stall1.getId());

        // Then: 平均评分应为 4.5
        assertThat(avgRating).isNotNull();
        assertThat(avgRating).isEqualTo(4.5);
    }

    @Test
    @DisplayName("聚合查询 - 计算档口平均评分 - 不存在的档口ID")
    void testGetAverageRatingByStallId_NonExistentStall() {
        // Given: 不存在的档口ID
        Long nonExistentStallId = 9999L;

        // When: 查询平均评分
        Double avgRating = reviewRepository.getAverageRatingByStallId(nonExistentStallId);

        // Then: 应返回 null
        assertThat(avgRating).isNull();
    }

    // ==================== 测试聚合查询：getRatingDistributionByStallId ====================

    @Test
    @DisplayName("聚合查询 - 获取评分分布 - 正常场景")
    void testGetRatingDistributionByStallId_Success() {
        // Given: 档口有不同评分的评价
        createReview(stall1, user1, 5.0, "优秀", LocalDateTime.now().minusDays(5));
        createReview(stall1, user2, 5.0, "优秀", LocalDateTime.now().minusDays(4));
        createReview(stall1, user1, 4.0, "不错", LocalDateTime.now().minusDays(3));
        createReview(stall1, user2, 3.0, "一般", LocalDateTime.now().minusDays(2));
        createReview(stall1, user1, 3.0, "一般", LocalDateTime.now().minusDays(1));
        entityManager.flush();

        // When: 查询评分分布
        List<Object[]> distribution = reviewRepository.getRatingDistributionByStallId(stall1.getId());

        // Then: 应返回评分分布（评分 + 数量）
        assertThat(distribution).hasSize(3); // 5.0, 4.0, 3.0

        // 验证返回结构
        for (Object[] row : distribution) {
            Double rating = (Double) row[0];
            Long count = (Long) row[1];

            if (rating.equals(5.0)) {
                assertThat(count).isEqualTo(2L);
            } else if (rating.equals(4.0)) {
                assertThat(count).isEqualTo(1L);
            } else if (rating.equals(3.0)) {
                assertThat(count).isEqualTo(2L);
            }
        }
    }

    @Test
    @DisplayName("聚合查询 - 获取评分分布 - 空数据场景")
    void testGetRatingDistributionByStallId_NoReviews() {
        // Given: 档口没有评价
        // When: 查询评分分布
        List<Object[]> distribution = reviewRepository.getRatingDistributionByStallId(stall1.getId());

        // Then: 应返回空列表
        assertThat(distribution).isEmpty();
    }

    @Test
    @DisplayName("聚合查询 - 获取评分分布 - 返回结构验证")
    void testGetRatingDistributionByStallId_ReturnStructure() {
        // Given: 一条评价
        createReview(stall1, user1, 4.5, "不错", LocalDateTime.now());
        entityManager.flush();

        // When: 查询评分分布
        List<Object[]> distribution = reviewRepository.getRatingDistributionByStallId(stall1.getId());

        // Then: 验证返回的 Object[] 结构
        assertThat(distribution).hasSize(1);

        Object[] row = distribution.get(0);
        assertThat(row).hasSize(2);
        assertThat(row[0]).isInstanceOf(Double.class); // rating
        assertThat(row[1]).isInstanceOf(Long.class);   // count
    }

    // ==================== 测试动态排序查询：findByStallIdWithSort ====================

    @Test
    @DisplayName("动态排序查询 - 按点赞数排序 - 正常场景")
    void testFindByStallIdWithSort_SortByLikesCount() {
        // Given: 档口有多条评价，点赞数不同
        Review review1 = createReview(stall1, user1, 4.0, "评价1", LocalDateTime.now().minusDays(3));
        review1.setLikesCount(10);
        entityManager.merge(review1);

        Review review2 = createReview(stall1, user2, 4.0, "评价2", LocalDateTime.now().minusDays(2));
        review2.setLikesCount(25);
        entityManager.merge(review2);

        Review review3 = createReview(stall1, user1, 4.0, "评价3", LocalDateTime.now().minusDays(1));
        review3.setLikesCount(5);
        entityManager.merge(review3);

        entityManager.flush();
        entityManager.clear();

        // When: 按点赞数排序查询
        Page<Review> reviews = reviewRepository.findByStallIdWithSort(
                stall1.getId(),
                "likesCount",
                PageRequest.of(0, 10)
        );

        // Then: 应按点赞数降序排列
        assertThat(reviews.getContent()).hasSize(3);
        assertThat(reviews.getContent().get(0).getLikesCount()).isEqualTo(25);
        assertThat(reviews.getContent().get(1).getLikesCount()).isEqualTo(10);
        assertThat(reviews.getContent().get(2).getLikesCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("动态排序查询 - 按创建时间排序 - 正常场景")
    void testFindByStallIdWithSort_SortByCreatedAt() {
        // Given: 档口有多条评价，创建时间不同
        LocalDateTime now = LocalDateTime.now();
        createReview(stall1, user1, 4.0, "最早", now.minusDays(3));
        createReview(stall1, user2, 4.0, "中间", now.minusDays(2));
        createReview(stall1, user1, 4.0, "最新", now.minusDays(1));
        entityManager.flush();

        // When: 按创建时间排序查询
        Page<Review> reviews = reviewRepository.findByStallIdWithSort(
                stall1.getId(),
                "createdAt",
                PageRequest.of(0, 10)
        );

        // Then: 应按创建时间降序排列（最新的在前）
        assertThat(reviews.getContent()).hasSize(3);
        assertThat(reviews.getContent().get(0).getComment()).isEqualTo("最新");
        assertThat(reviews.getContent().get(1).getComment()).isEqualTo("中间");
        assertThat(reviews.getContent().get(2).getComment()).isEqualTo("最早");
    }

    @Test
    @DisplayName("动态排序查询 - 无效sortBy参数")
    void testFindByStallIdWithSort_InvalidSortBy() {
        // Given: 档口有评价
        createReview(stall1, user1, 4.0, "评价1", LocalDateTime.now().minusDays(2));
        createReview(stall1, user2, 4.0, "评价2", LocalDateTime.now().minusDays(1));
        entityManager.flush();

        // When: 使用无效的 sortBy 参数
        Page<Review> reviews = reviewRepository.findByStallIdWithSort(
                stall1.getId(),
                "invalidSort",
                PageRequest.of(0, 10)
        );

        // Then: 应返回数据但排序未应用（CASE WHEN 条件都不满足）
        assertThat(reviews.getContent()).hasSize(2);
        // 注意：当 sortBy 无效时，排序结果可能不确定
    }

    @Test
    @DisplayName("动态排序查询 - 分页功能测试")
    void testFindByStallIdWithSort_Pagination() {
        // Given: 档口有5条评价
        for (int i = 1; i <= 5; i++) {
            Review review = createReview(stall1, user1, 4.0, "评价" + i, LocalDateTime.now().minusDays(i));
            review.setLikesCount(i * 10);
            entityManager.merge(review);
        }
        entityManager.flush();

        // When: 查询第一页（2条）
        Page<Review> page1 = reviewRepository.findByStallIdWithSort(
                stall1.getId(),
                "likesCount",
                PageRequest.of(0, 2)
        );

        // Then: 应返回点赞数最高的2条
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("动态排序查询 - 空结果场景")
    void testFindByStallIdWithSort_NoReviews() {
        // Given: 档口没有评价
        // When: 查询评价
        Page<Review> reviews = reviewRepository.findByStallIdWithSort(
                stall1.getId(),
                "likesCount",
                PageRequest.of(0, 10)
        );

        // Then: 应返回空结果
        assertThat(reviews.getContent()).isEmpty();
        assertThat(reviews.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("动态排序查询 - 档口隔离测试")
    void testFindByStallIdWithSort_StallIsolation() {
        // Given: 两个档口有不同的评价
        createReview(stall1, user1, 4.0, "档口1评价", LocalDateTime.now());
        createReview(stall2, user2, 4.0, "档口2评价", LocalDateTime.now());
        entityManager.flush();

        // When: 查询档口1的评价
        Page<Review> stall1Reviews = reviewRepository.findByStallIdWithSort(
                stall1.getId(),
                "createdAt",
                PageRequest.of(0, 10)
        );

        // Then: 只应返回档口1的评价
        assertThat(stall1Reviews.getContent()).hasSize(1);
        assertThat(stall1Reviews.getContent().get(0).getComment()).isEqualTo("档口1评价");
    }

    // ==================== 辅助方法 ====================

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(UserRole.ROLE_USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return entityManager.persist(user);
    }

    private Stall createStall(String name) {
        Stall stall = new Stall();
        stall.setName(name);
        stall.setCuisineType("中餐");
        stall.setAverageRating(0.0);
        stall.setReviewCount(0);
        stall.setCreatedAt(LocalDateTime.now());
        stall.setUpdatedAt(LocalDateTime.now());
        return entityManager.persist(stall);
    }

    private Review createReview(Stall stall, User user, Double rating, String comment, LocalDateTime createdAt) {
        Review review = new Review();
        review.setStall(stall);
        review.setUser(user);
        review.setAuthor(user.getUsername());
        review.setRating(rating);
        review.setComment(comment);
        review.setLikesCount(0);
        review.setProcessed(false);
        review.setCreatedAt(createdAt);
        review.setUpdatedAt(createdAt);
        return entityManager.persist(review);
    }
}

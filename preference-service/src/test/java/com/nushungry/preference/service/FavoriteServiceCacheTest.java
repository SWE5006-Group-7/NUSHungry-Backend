package com.nushungry.preference.service;

import com.nushungry.preference.entity.Favorite;
import com.nushungry.preference.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FavoriteService 缓存功能测试
 * 
 * 测试 Redis 缓存是否正常工作
 * 测试缓存的存储和清除逻辑
 */
@SpringBootTest
@ActiveProfiles("test")
class FavoriteServiceCacheTest {

    @Autowired
    private FavoriteService favoriteService;

    @MockBean
    private FavoriteRepository favoriteRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 清除所有缓存
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        reset(favoriteRepository);
    }

    @Test
    void testListFavoritesCaching() {
        // Given
        Long userId = 1L;
        Favorite fav1 = new Favorite();
        fav1.setUserId(userId);
        fav1.setStallId(10L);
        fav1.setCreatedAt(System.currentTimeMillis());

        Favorite fav2 = new Favorite();
        fav2.setUserId(userId);
        fav2.setStallId(20L);
        fav2.setCreatedAt(System.currentTimeMillis());

        List<Favorite> favorites = Arrays.asList(fav1, fav2);
        when(favoriteRepository.findByUserId(userId)).thenReturn(favorites);

        // When - 第一次调用
        List<Long> result1 = favoriteService.listFavorites(userId);
        
        // When - 第二次调用（应该从缓存获取）
        List<Long> result2 = favoriteService.listFavorites(userId);

        // Then
        assertThat(result1).hasSize(2).containsExactly(10L, 20L);
        assertThat(result2).hasSize(2).containsExactly(10L, 20L);
        
        // 验证 repository 只被调用一次（第二次从缓存获取）
        verify(favoriteRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testSortedFavoritesCaching() {
        // Given
        Long userId = 1L;
        Favorite fav1 = new Favorite();
        fav1.setUserId(userId);
        fav1.setStallId(10L);
        fav1.setCreatedAt(1000L);
        fav1.setSortOrder(0);

        Favorite fav2 = new Favorite();
        fav2.setUserId(userId);
        fav2.setStallId(20L);
        fav2.setCreatedAt(2000L);
        fav2.setSortOrder(0);

        // sortedFavorites 使用不同的查询方法
        List<Favorite> favorites = Arrays.asList(fav2, fav1); // 已经按时间倒序排列
        when(favoriteRepository.findByUserIdOrderBySortOrderDescCreatedAtDesc(userId)).thenReturn(favorites);

        // When - 第一次调用
        List<Long> result1 = favoriteService.sortedFavorites(userId);

        // When - 第二次调用（应该从缓存获取）
        List<Long> result2 = favoriteService.sortedFavorites(userId);

        // Then - 应该按时间倒序排列
        assertThat(result1).hasSize(2).containsExactly(20L, 10L);
        assertThat(result2).hasSize(2).containsExactly(20L, 10L);

        // 验证 repository 只被调用一次（sortedFavorites 使用专门的方法）
        verify(favoriteRepository, times(1)).findByUserIdOrderBySortOrderDescCreatedAtDesc(userId);
    }

    @Test
    void testAddFavoriteClearsCaches() {
        // Given
        Long userId = 1L;
        Long stallId = 30L;

        Favorite fav1 = new Favorite();
        fav1.setUserId(userId);
        fav1.setStallId(10L);

        List<Favorite> favorites = Arrays.asList(fav1);
        when(favoriteRepository.findByUserId(userId)).thenReturn(favorites);
        when(favoriteRepository.findByUserIdOrderBySortOrderDescCreatedAtDesc(userId)).thenReturn(favorites);
        when(favoriteRepository.existsByUserIdAndStallId(userId, stallId)).thenReturn(false);

        // When - 填充缓存
        favoriteService.listFavorites(userId);
        favoriteService.sortedFavorites(userId);

        // listFavorites 使用 findByUserId, sortedFavorites 使用专门的方法
        verify(favoriteRepository, times(1)).findByUserId(userId);
        verify(favoriteRepository, times(1)).findByUserIdOrderBySortOrderDescCreatedAtDesc(userId);

        // When - 添加收藏（应该清除缓存）
        favoriteService.addFavorite(userId, stallId);

        // When - 再次查询（应该重新从数据库获取）
        favoriteService.listFavorites(userId);
        favoriteService.sortedFavorites(userId);

        // Then - 验证缓存被清除，重新从数据库查询
        verify(favoriteRepository, times(2)).findByUserId(userId);
        verify(favoriteRepository, times(2)).findByUserIdOrderBySortOrderDescCreatedAtDesc(userId);
    }

    @Test
    void testRemoveFavoriteClearsCaches() {
        // Given
        Long userId = 1L;
        Long stallId = 10L;

        Favorite fav1 = new Favorite();
        fav1.setUserId(userId);
        fav1.setStallId(stallId);

        List<Favorite> favorites = Arrays.asList(fav1);
        when(favoriteRepository.findByUserId(userId)).thenReturn(favorites);
        when(favoriteRepository.findByUserIdOrderBySortOrderDescCreatedAtDesc(userId)).thenReturn(favorites);

        // When - 填充缓存
        favoriteService.listFavorites(userId);
        favoriteService.sortedFavorites(userId);

        verify(favoriteRepository, times(1)).findByUserId(userId);
        verify(favoriteRepository, times(1)).findByUserIdOrderBySortOrderDescCreatedAtDesc(userId);

        // When - 删除收藏（应该清除缓存）
        favoriteService.removeFavorite(userId, stallId);

        // When - 再次查询
        favoriteService.listFavorites(userId);
        favoriteService.sortedFavorites(userId);

        // Then - 验证缓存被清除
        verify(favoriteRepository, times(2)).findByUserId(userId);
        verify(favoriteRepository, times(2)).findByUserIdOrderBySortOrderDescCreatedAtDesc(userId);
    }

    @Test
    void testBatchRemoveClearsCaches() {
        // Given
        Long userId = 1L;
        List<Long> stallIds = Arrays.asList(10L, 20L);
        
        List<Favorite> favorites = Arrays.asList(new Favorite());
        when(favoriteRepository.findByUserId(userId)).thenReturn(favorites);

        // When - 填充缓存
        favoriteService.listFavorites(userId);
        
        verify(favoriteRepository, times(1)).findByUserId(userId);

        // When - 批量删除（应该清除缓存）
        favoriteService.batchRemove(userId, stallIds);

        // When - 再次查询
        favoriteService.listFavorites(userId);

        // Then - 验证缓存被清除
        verify(favoriteRepository, times(2)).findByUserId(userId);
    }

    @Test
    void testCacheManagerAvailable() {
        // 验证 CacheManager 是否可用（在测试环境中可能不可用）
        if (cacheManager != null) {
            assertThat(cacheManager.getCacheNames()).contains("favorites");
        }
    }
}
